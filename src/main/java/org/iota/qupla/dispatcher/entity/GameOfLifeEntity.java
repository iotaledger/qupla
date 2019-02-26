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
  private static final int HASH_SIZE = 243;
  private static final int MAP_SIZE = 81;

  public TritVector currentId = new TritVector(HASH_SIZE, '0');
  public TritVector currentMap = new TritVector(MAP_SIZE * MAP_SIZE, '0');
  public JTextField entry;
  public JFrame frame;
  public Environment golGen;
  public Environment golHash;
  public Environment golIds;
  public Environment golSend;
  public Environment golView;
  public BufferedImage mapImage;
  public JPanel mapView;

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

    mapImage = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_3BYTE_BGR);

    mapView = new JPanel();
    mapView.setPreferredSize(new Dimension(200, 200));
    mapView.setVisible(true);
    final MouseInputAdapter mouseAdapter = getMouseInputAdapter();
    mapView.addMouseListener(mouseAdapter);
    mapView.addMouseMotionListener(mouseAdapter);

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
    frame.add(mapView, BorderLayout.CENTER);
    frame.addComponentListener(new ComponentAdapter()
    {
      public void componentResized(ComponentEvent evt)
      {
        drawMapImage();
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

  private void drawMapImage()
  {
    final Graphics graphics = mapImage.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, MAP_SIZE, MAP_SIZE);
    graphics.setColor(Color.DARK_GRAY);
    final String trits = currentMap.trits();
    int offset = 0;
    for (int y = 0; y < MAP_SIZE; y++)
    {
      for (int x = 0; x < MAP_SIZE; x++)
      {
        if (trits.charAt(offset + x) == '1')
        {
          graphics.drawRect(x, y, 0, 0);
        }
      }

      offset += MAP_SIZE;
    }

    final Dimension size = mapView.getSize();
    mapView.getGraphics().drawImage(mapImage, 0, 0, size.width, size.height, null);
  }

  private MouseInputAdapter getMouseInputAdapter()
  {
    return new MouseInputAdapter()
    {
      private char cell;

      private int getOffset(final MouseEvent mouseEvent)
      {
        final Point point = mouseEvent.getPoint();
        final Dimension size = mapView.getSize();
        final int x = MAP_SIZE * point.x / size.width;
        final int y = MAP_SIZE * point.y / size.height;
        return y * MAP_SIZE + x;
      }

      @Override
      public void mouseDragged(final MouseEvent mouseEvent)
      {
        if (cell == 0)
        {
          return;
        }

        final int offset = getOffset(mouseEvent);
        final String trits = currentMap.trits();
        currentMap = new TritVector(trits.substring(0, offset) + cell + trits.substring(offset + 1));
        golView.affect(TritVector.concat(currentId, currentMap), 0);
      }

      @Override
      public void mousePressed(final MouseEvent mouseEvent)
      {
        if (mouseEvent.getButton() != 1)
        {
          golGen.affect(TritVector.concat(currentId, currentMap), 0);
          return;
        }

        final int offset = getOffset(mouseEvent);
        final String trits = currentMap.trits();
        cell = trits.charAt(offset) == '1' ? '0' : '1';
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
        golSend.affect(TritVector.concat(currentId, currentMap), 0);
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
      currentMap = effect.slice(HASH_SIZE, MAP_SIZE * MAP_SIZE);
      drawMapImage();
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
      golIds.affect(TritVector.concat(new TritVector(1, '-'), currentId), 0);
    }

    // make new id
    currentId = TritConverter.trytesToVector(trytes).slicePadded(0, 243);

    // save new id
    if (!currentId.isZero())
    {
      golIds.affect(TritVector.concat(new TritVector(1, '1'), currentId), 0);
    }
  }
}
