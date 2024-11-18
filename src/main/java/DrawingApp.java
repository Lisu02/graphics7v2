import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DrawingApp extends JFrame {
    private List<PolygonShape> shapes = new ArrayList<>();
    private PolygonShape currentShape;
    private PolygonShape selectedShape;

    // Tryby interakcji
    private enum Mode { NONE, MOVE, ROTATE, SCALE }

    private Mode currentMode = Mode.NONE;
    private Point2D referencePoint;

    public DrawingApp() {
        setTitle("Transformacje Figur");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        DrawingPanel canvas = new DrawingPanel();
        add(canvas, BorderLayout.CENTER);
        add(createControlPanel(canvas), BorderLayout.SOUTH);
    }

    private JPanel createControlPanel(DrawingPanel canvas) {
        JPanel panel = new JPanel();

        JTextField txField = new JTextField(5);
        JTextField tyField = new JTextField(5);
        JTextField angleField = new JTextField(5);
        JTextField scaleField = new JTextField(5);

        // Przesunięcie
        JButton moveButton = new JButton("Przesuń");
        moveButton.addActionListener(e -> {
            try {
                int tx = Integer.parseInt(txField.getText());
                int ty = Integer.parseInt(tyField.getText());
                if (selectedShape != null) {
                    selectedShape.applyTransformation(TransformationUtil.createTranslation(tx, ty));
                    canvas.repaint();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Podaj poprawne liczby całkowite dla przesunięcia.");
            }
        });

        // Obrót
        JButton rotateButton = new JButton("Obróć");
        rotateButton.addActionListener(e -> {
            try {
                int angle = Integer.parseInt(angleField.getText());
                if (selectedShape != null) {
                    Point2D center = referencePoint != null ? referencePoint : selectedShape.getCenter();
                    selectedShape.applyTransformation(
                            TransformationUtil.createRotation(Math.toRadians(angle), center.getX(), center.getY())
                    );
                    canvas.repaint();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Podaj poprawną liczbę całkowitą dla kąta obrotu.");
            }
        });

        // Skalowanie
        JButton scaleButton = new JButton("Skaluj");
        scaleButton.addActionListener(e -> {
            try {
                int scale = Integer.parseInt(scaleField.getText());
                if (scale <= 0) {
                    throw new NumberFormatException("Skala musi być większa od zera.");
                }
                if (selectedShape != null) {
                    Point2D center = referencePoint != null ? referencePoint : selectedShape.getCenter();
                    selectedShape.applyTransformation(
                            TransformationUtil.createScaling(scale, scale, center.getX(), center.getY())
                    );
                    canvas.repaint();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Podaj poprawną liczbę całkowitą większą od zera dla skali.");
            }
        });

        // Tryby myszy
        JToggleButton moveMode = new JToggleButton("Tryb Przesuwania");
        moveMode.addActionListener(e -> currentMode = moveMode.isSelected() ? Mode.MOVE : Mode.NONE);

        JToggleButton rotateMode = new JToggleButton("Tryb Obracania");
        rotateMode.addActionListener(e -> currentMode = rotateMode.isSelected() ? Mode.ROTATE : Mode.NONE);

        JToggleButton scaleMode = new JToggleButton("Tryb Skalowania");
        scaleMode.addActionListener(e -> currentMode = scaleMode.isSelected() ? Mode.SCALE : Mode.NONE);

        // Czyszczenie
        JButton clearButton = new JButton("Wyczyść");
        clearButton.addActionListener(e -> {
            shapes.clear();
            selectedShape = null;
            canvas.repaint();
        });

        panel.add(new JLabel("tx:"));
        panel.add(txField);
        panel.add(new JLabel("ty:"));
        panel.add(tyField);
        panel.add(moveButton);
        panel.add(new JLabel("Kąt:"));
        panel.add(angleField);
        panel.add(rotateButton);
        panel.add(new JLabel("Skala:"));
        panel.add(scaleField);
        panel.add(scaleButton);
        panel.add(moveMode);
        panel.add(rotateMode);
        panel.add(scaleMode);
        panel.add(clearButton);

        return panel;
    }

    class DrawingPanel extends JPanel {
        private Point2D dragStart;

        public DrawingPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point2D point = e.getPoint();
                    if (currentShape == null) {
                        currentShape = new PolygonShape(new ArrayList<>());
                        shapes.add(currentShape);
                    }
                    currentShape.getVertices().add(point);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (currentShape != null && currentShape.getVertices().size() > 2) {
                        currentShape = null; // Zakończ rysowanie
                    } else if (currentMode == Mode.NONE) {
                        // Wybierz figurę
                        for (PolygonShape shape : shapes) {
                            if (shape.contains(e.getPoint())) {
                                selectedShape = shape;
                                break;
                            }
                        }
                    } else {
                        referencePoint = e.getPoint();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (selectedShape != null) {
                        Point2D dragEnd = e.getPoint();
                        if (currentMode == Mode.MOVE) {
                            double dx = dragEnd.getX() - dragStart.getX();
                            double dy = dragEnd.getY() - dragStart.getY();
                            selectedShape.applyTransformation(TransformationUtil.createTranslation(dx, dy));
                        } else if (currentMode == Mode.ROTATE && referencePoint != null) {
                            double angle = Math.atan2(dragEnd.getY() - referencePoint.getY(), dragEnd.getX() - referencePoint.getX())
                                    - Math.atan2(dragStart.getY() - referencePoint.getY(), dragStart.getX() - referencePoint.getX());
                            selectedShape.applyTransformation(TransformationUtil.createRotation(angle, referencePoint.getX(), referencePoint.getY()));
                        } else if (currentMode == Mode.SCALE && referencePoint != null) {
                            double scaleFactor = dragEnd.distance(referencePoint) / dragStart.distance(referencePoint);
                            selectedShape.applyTransformation(TransformationUtil.createScaling(scaleFactor, scaleFactor, referencePoint.getX(), referencePoint.getY()));
                        }
                        dragStart = dragEnd;
                        repaint();
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    dragStart = e.getPoint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            for (PolygonShape shape : shapes) {
                shape.draw(g2d);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawingApp().setVisible(true));
    }
}

// Klasa do reprezentacji wielokątów i transformacji - bez zmian
// Klasa TransformationUtil - bez zmian
// Klasa do reprezentacji wielokątów
class PolygonShape implements Serializable {
    private List<Point2D> vertices;

    public PolygonShape(List<Point2D> vertices) {
        this.vertices = vertices;
    }

    public List<Point2D> getVertices() {
        return vertices;
    }

    public void applyTransformation(AffineTransform transform) {
        for (int i = 0; i < vertices.size(); i++) {
            Point2D point = vertices.get(i);
            Point2D transformed = transform.transform(point, null);
            vertices.set(i, transformed);
        }
    }

    public Point2D getCenter() {
        double x = 0, y = 0;
        for (Point2D point : vertices) {
            x += point.getX();
            y += point.getY();
        }
        return new Point2D.Double(x / vertices.size(), y / vertices.size());
    }

    public boolean contains(Point2D point) {
        Polygon polygon = new Polygon();
        for (Point2D vertex : vertices) {
            polygon.addPoint((int) vertex.getX(), (int) vertex.getY());
        }
        return polygon.contains(point);
    }

    public void draw(Graphics2D g2d) {
        for (int i = 0; i < vertices.size(); i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get((i + 1) % vertices.size());
            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
        }
    }
}

// Klasa do transformacji
class TransformationUtil {
    public static AffineTransform createTranslation(double tx, double ty) {
        return AffineTransform.getTranslateInstance(tx, ty);
    }

    public static AffineTransform createRotation(double angle, double px, double py) {
        AffineTransform transform = new AffineTransform();
        transform.translate(px, py);
        transform.rotate(angle);
        transform.translate(-px, -py);
        return transform;
    }

    public static AffineTransform createScaling(double sx, double sy, double px, double py) {
        AffineTransform transform = new AffineTransform();
        transform.translate(px, py);
        transform.scale(sx, sy);
        transform.translate(-px, -py);
        return transform;
    }
}