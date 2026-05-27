package UI.components;

import UI.theme.ThemeManager;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Rectangle;

/**
 * Modern enterprise table with alternating rows, sticky header, hover highlight,
 * and a rounded container. Wrap any DefaultTableModel here for a premium look.
 */
public class ModernTable extends JPanel {

    private final JTable table;
    private final DefaultTableModel model;
    private int hoveredRow = -1;
    private int highlightedRow = -1;
    private final Color highlightColor = new Color(0xD4EDDA); // soft green
    private boolean[] editableColumns;

    public ModernTable(String[] columns) {
        this.editableColumns = new boolean[columns.length];
        this.model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                if (c >= 0 && c < editableColumns.length) {
                    return editableColumns[c];
                }
                return false;
            }
        };
        this.table = buildTable();

        setOpaque(false);
        setLayout(new BorderLayout());

        JScrollPane scroll = new JScrollPane(table);
        // Add padding so the square viewport does not clip the parent's rounded corners (radius 14)
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scroll.setBackground(Color.WHITE);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getViewport().setOpaque(true);
        // Enable backing store for smooth hardware-accelerated scrolling
        scroll.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setOpaque(false);

        add(scroll, BorderLayout.CENTER);
    }

    private JTable buildTable() {
        JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(UIConstants.TABLE_SELECTION);
                    c.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                } else if (row == highlightedRow) {
                    c.setBackground(highlightColor);
                    c.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                } else if (row == hoveredRow) {
                    c.setBackground(UIConstants.TABLE_ROW_HOVER);
                    c.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                } else {
                    c.setBackground(row % 2 == 0 ? UIConstants.TABLE_ROW_EVEN
                                                  : UIConstants.TABLE_ROW_ODD);
                    c.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                }
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
                return c;
            }
        };

        t.setFont(UIConstants.FONT_NORMAL);
        t.setRowHeight(38);
        t.setShowGrid(true);
        t.setGridColor(UIConstants.TABLE_GRID);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFocusable(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header
        JTableHeader header = t.getTableHeader();
        header.setFont(UIConstants.FONT_NORMAL_BOLD);
        header.setBackground(UIConstants.TABLE_HEADER_BG);
        header.setForeground(UIConstants.TABLE_HEADER_FG);
        header.setPreferredSize(new Dimension(header.getWidth(), 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(JLabel.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            }
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setBackground(UIConstants.TABLE_HEADER_BG);
                setForeground(UIConstants.TABLE_HEADER_FG);
                setFont(UIConstants.FONT_NORMAL_BOLD);
                return this;
            }
        });

        // Hover tracking
        t.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = t.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    int oldHovered = hoveredRow;
                    hoveredRow = row;
                    if (oldHovered != -1 && oldHovered < t.getRowCount()) {
                        t.repaint(t.getCellRect(oldHovered, 0, true));
                    }
                    if (hoveredRow != -1 && hoveredRow < t.getRowCount()) {
                        t.repaint(t.getCellRect(hoveredRow, 0, true));
                    }
                }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                int oldHovered = hoveredRow;
                hoveredRow = -1;
                if (oldHovered != -1 && oldHovered < t.getRowCount()) {
                    t.repaint(t.getCellRect(oldHovered, 0, true));
                }
            }
        });

        // Set backgrounds white & opaque for hardware scroll acceleration
        t.setBackground(Color.WHITE);
        t.setOpaque(true);

        return t;
    }

    /** Add a row of data. */
    public void addRow(Object[] rowData) { model.addRow(rowData); }

    /** Clear all rows. */
    public void clearRows() { model.setRowCount(0); }

    /** Get the number of rows. */
    public int getRowCount() { return model.getRowCount(); }

    /** Get value at specific cell. */
    public Object getValueAt(int row, int col) { return model.getValueAt(row, col); }

    /** Set preferred column widths. */
    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Set whether a column is editable. */
    public void setColumnEditable(int col, boolean editable) {
        if (col >= 0 && col < editableColumns.length) {
            editableColumns[col] = editable;
        }
    }

    /** Set a custom cell renderer on a specific column. */
    public void setColumnRenderer(int col, TableCellRenderer renderer) {
        table.getColumnModel().getColumn(col).setCellRenderer(renderer);
    }

    /** Access the underlying JTable for additional customization. */
    public JTable getTable() { return table; }

    /** Access the underlying model. */
    public DefaultTableModel getModel() { return model; }

    /** Get selected row index (-1 if none). */
    public int getSelectedRow() { return table.getSelectedRow(); }

    /**
     * Briefly highlights a view-row in soft green for 1600ms then resets.
     * Call with the VIEW row index (after sorting).
     */
    public void flashRow(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) return;
        highlightedRow = viewRow;
        table.repaint();
        // Scroll viewport to show the row
        Rectangle rect = table.getCellRect(viewRow, 0, true);
        table.scrollRectToVisible(rect);
        // Auto-reset highlight after 1600ms
        Timer t = new Timer(1600, e -> {
            highlightedRow = -1;
            table.repaint();
        });
        t.setRepeats(false);
        t.start();
    }

    /** Scrolls the table viewport so the given view-row is visible. */
    public void scrollToRow(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) return;
        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ThemeManager.drawCardShadow(g2, 3, 5, getWidth() - 8, getHeight() - 10, 14);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(3, 3, getWidth() - 8, getHeight() - 8, 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}
