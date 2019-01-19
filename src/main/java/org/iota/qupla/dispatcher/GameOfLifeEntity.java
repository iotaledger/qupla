package org.iota.qupla.dispatcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.NumberFormatter;
import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.helper.TritVector;

public class GameOfLifeEntity extends Entity implements PropertyChangeListener
{
  private static final int ID_SIZE = 9;
  private static final int MAP_SIZE = 81;

  public TritVector currentMap = new TritVector(ID_SIZE + MAP_SIZE * MAP_SIZE, '0');
  public JTextField entry;
  public JFrame frame;
  public Environment gameOfLife;
  public Environment gameOfLifeResult;
  public BufferedImage mapImage;
  public JPanel mapView;

  public GameOfLifeEntity()
  {
    super(0);

    final Dispatcher dispatcher = Dispatcher.getInstance();
    gameOfLife = dispatcher.getEnvironment("gameOfLife", null);
    gameOfLifeResult = dispatcher.getEnvironment("gameOfLifeResult", null);
    affect(gameOfLife, 0);
    join(gameOfLifeResult);

    mapView = new JPanel();
    mapView.setPreferredSize(new Dimension(200, 200));
    mapView.setVisible(true);
    final MouseInputAdapter mouseAdapter = getMouseInputAdapter();
    mapView.addMouseListener(mouseAdapter);
    mapView.addMouseMotionListener(mouseAdapter);

    final JLabel label = new JLabel();
    label.setText("GoL ID:");

    entry = new JFormattedTextField(getNumberFormatter());
    entry.addPropertyChangeListener(this);

    final JPanel idPanel = new JPanel();
    idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
    idPanel.add(label);
    idPanel.add(entry);

    frame = new JFrame("Game of Life");
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

  private void drawMapImage()
  {
    mapImage = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_3BYTE_BGR);
    final Graphics graphics = mapImage.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, MAP_SIZE, MAP_SIZE);
    graphics.setColor(Color.DARK_GRAY);
    final String trits = currentMap.trits();
    for (int y = 0; y < MAP_SIZE; y++)
    {
      final int offset = ID_SIZE + y * MAP_SIZE;
      for (int x = 0; x < MAP_SIZE; x++)
      {
        if (trits.charAt(offset + x) == '1')
        {
          graphics.drawRect(x, y, 0, 0);
        }
      }
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
        return ID_SIZE + y * MAP_SIZE + x;
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
        gameOfLifeResult.queueEntityEvents(currentMap, 0);
      }

      @Override
      public void mousePressed(final MouseEvent mouseEvent)
      {
        if (mouseEvent.getButton() != 1)
        {
          nextGeneration();
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
      }
    };
  }

  private NumberFormatter getNumberFormatter()
  {
    final NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
    formatter.setValueClass(Integer.class);
    formatter.setMinimum(0);
    formatter.setMaximum(999);
    formatter.setAllowsInvalid(false);
    formatter.setCommitsOnValidEdit(true);
    return formatter;
  }

  private void nextGeneration()
  {
    queueEffectEvents(currentMap);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
  {
    final int id = entry.getText().length() == 0 ? 0 : Integer.parseInt(entry.getText());
    if (id >= 0 && id <= 999)
    {
      final String idTrits = TritConverter.fromLong(id);
      final TritVector idVector = new TritVector(idTrits).slicePadded(0, ID_SIZE);
      currentMap = TritVector.concat(idVector, currentMap.slice(ID_SIZE, MAP_SIZE * MAP_SIZE));
    }
  }

  @Override
  public TritVector runWave(final TritVector inputValue)
  {
    final String inputId = inputValue.slice(0, ID_SIZE).trits();
    final String currentId = currentMap.slice(0, ID_SIZE).trits();
    if (inputId.equals(currentId))
    {
      currentMap = inputValue;
      drawMapImage();
    }

    // return null, no need to propagate, we only log inputValue
    return nullVector;
  }
}
