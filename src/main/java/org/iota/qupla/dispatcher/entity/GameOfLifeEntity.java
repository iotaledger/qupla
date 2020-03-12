package org.iota.qupla.dispatcher.entity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.dispatcher.Environment;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class GameOfLifeEntity extends Entity
{
  private static final int GRID_SIZE = 81;
  private static final int HASH_SIZE = 243;
  public TritVector currentGrid = new TritVector(GRID_SIZE * GRID_SIZE, TritVector.TRIT_ZERO);
  public TritVector currentId = new TritVector(HASH_SIZE, TritVector.TRIT_ZERO);
  public JTextField entry;
  public JFrame frame;
  public Environment golGen;
  public Environment golHash;
  public Environment golIds;
  public Environment golSend;
  public Environment golView;
  public BufferedImage gridImage;
  public JPanel gridView;

  public GameOfLifeEntity()
  {
    super(0);

    final Dispatcher dispatcher = Dispatcher.getInstance();
    golGen = dispatcher.getEnvironment("GolGen", null);
    golHash = dispatcher.getEnvironment("GolHash", null);
    golIds = dispatcher.getEnvironment("GolIds", null);
    golSend = dispatcher.getEnvironment("GolSend", null);
    golView = dispatcher.getEnvironment("GolView", null);
    join(golView);

    gridImage = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_3BYTE_BGR);

    gridView = new JPanel();
    gridView.setPreferredSize(new Dimension(200, 200));
    gridView.setVisible(true);
    final MouseInputAdapter mouseAdapter = getMouseInputAdapter();
    gridView.addMouseListener(mouseAdapter);
    gridView.addMouseMotionListener(mouseAdapter);

    final JLabel label = new JLabel();
    label.setText("GoL ID:");

    entry = new JTextField();
    addChangeListener();

    final JPanel idPanel = new JPanel();
    idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
    idPanel.add(label);
    idPanel.add(entry);

    frame = new JFrame("Game of Life");
    frame.addWindowListener(ViewEntity.windowAdapter);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(idPanel, BorderLayout.PAGE_START);
    frame.add(gridView, BorderLayout.CENTER);
    frame.addComponentListener(new ComponentAdapter()
    {
      public void componentResized(ComponentEvent evt)
      {
        drawGridImage();
      }
    });
    frame.setVisible(true);
    frame.setSize(400, 400);
  }

  private void addChangeListener()
  {
    entry.getDocument().addDocumentListener(new DocumentListener()
    {
      public void changedUpdate(DocumentEvent e)
      {
        onEntryChange();
      }

      public void insertUpdate(DocumentEvent e)
      {
        onEntryChange();
      }

      public void removeUpdate(DocumentEvent e)
      {
        onEntryChange();
      }
    });
  }

  private void drawGridImage()
  {
    final Graphics graphics = gridImage.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, GRID_SIZE, GRID_SIZE);
    graphics.setColor(Color.DARK_GRAY);
    final byte[] trits = currentGrid.trits();
    int offset = 0;
    for (int y = 0; y < GRID_SIZE; y++)
    {
      for (int x = 0; x < GRID_SIZE; x++)
      {
        if (trits[offset + x] == '1')
        {
          graphics.drawRect(x, y, 0, 0);
        }
      }

      offset += GRID_SIZE;
    }

    final Dimension size = gridView.getSize();
    gridView.getGraphics().drawImage(gridImage, 0, 0, size.width, size.height, null);
  }

  private MouseInputAdapter getMouseInputAdapter()
  {
    return new MouseInputAdapter()
    {
      private char cell;

      private int getOffset(final MouseEvent mouseEvent)
      {
        final Point point = mouseEvent.getPoint();
        final Dimension size = gridView.getSize();
        final int x = GRID_SIZE * point.x / size.width;
        final int y = GRID_SIZE * point.y / size.height;
        return y * GRID_SIZE + x;
      }

      @Override
      public void mouseDragged(final MouseEvent mouseEvent)
      {
        if (cell == 0)
        {
          return;
        }

        final int offset = getOffset(mouseEvent);
        final byte[] trits = currentGrid.trits();
        trits[offset] = (byte) cell;
        currentGrid = new TritVector(trits);
        golView.affect(TritVector.concat(currentId, currentGrid), 0);
      }

      @Override
      public void mousePressed(final MouseEvent mouseEvent)
      {
        if (mouseEvent.getButton() != 1)
        {
          golGen.affect(TritVector.concat(currentId, currentGrid), 0);
          return;
        }

        final int offset = getOffset(mouseEvent);
        cell = currentGrid.trit(offset) == '1' ? '0' : '1';
        mouseDragged(mouseEvent);
      }

      @Override
      public void mouseReleased(final MouseEvent mouseEvent)
      {
        if (mouseEvent.getButton() != 1)
        {
          return;
        }

        cell = 0;
        golSend.affect(TritVector.concat(currentId, currentGrid), 0);
      }
    };
  }

  @Override
  public TritVector onEffect(final TritVector effect)
  {
    //TODO fix this
    final TritVector inputId = effect.slice(0, HASH_SIZE);
    if (inputId.equals(currentId))
    {
      currentGrid = effect.slice(HASH_SIZE, GRID_SIZE * GRID_SIZE);
      drawGridImage();
    }

    // return null, no need to propagate anything
    return null;
  }

  private void onEntryChange()
  {
    final String id = entry.getText();
    String trytes = "";
    for (int i = 0; i < id.length(); i++)
    {
      final char c = id.charAt(i);
      trytes += "9ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(c & 0x0f);
      trytes += "9ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt((c >> 4) & 0x0f);
    }

    // remove previous id
    if (!currentId.isZero())
    {
      golIds.affect(TritVector.concat(new TritVector(1, TritVector.TRIT_MIN), currentId), 0);
    }

    // make new id
    currentId = TritConverter.trytesToVector(trytes).slicePadded(0, 243);

    // save new id
    if (!currentId.isZero())
    {
      golIds.affect(TritVector.concat(new TritVector(1, TritVector.TRIT_ONE), currentId), 0);
    }
  }
}
