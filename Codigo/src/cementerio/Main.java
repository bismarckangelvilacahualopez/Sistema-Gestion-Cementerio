package cementerio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.geom.RoundRectangle2D;
import java.util.regex.Pattern;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Sistema de gestión de cementerio municipal. Java 17+, sin librerías externas. */
public final class Main extends JFrame {
    private static final long serialVersionUID = 1L;
    private final Data db = Data.load();
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, JTable> grids = new LinkedHashMap<>();
    private final Map<String, JScrollPane> scrolls = new LinkedHashMap<>();
    private final Map<String, JPanel> panels = new LinkedHashMap<>();
    private final JLabel status = new JLabel("  Sistema listo");
    private final Color NAVY = new Color(18, 32, 49);
    private final Color NAVY_LIGHT = new Color(29, 47, 66);
    private final Color BLUE = new Color(22, 125, 123);
    private final Color BACKGROUND = new Color(245, 248, 250);
    private final Color INK = new Color(34, 49, 64);
    private final JLabel sectionTitle = new JLabel("Dolientes");
    private final JLabel sectionInfo = new JLabel("Gestión de registros y operaciones");
    private final JLabel counterDolientes = new JLabel("0");
    private final JLabel counterDifuntos = new JLabel("0");
    private final JLabel counterNichos = new JLabel("0");
    private final JLabel counterSepelios = new JLabel("0");
    private final JLabel counterVencimientos = new JLabel("0");
    private final Map<String, JButton> menuButtons = new LinkedHashMap<>();
    private final JTextField search = new JTextField(25);
    private final String[] modules = {"Dolientes", "Difuntos", "Pabellones", "Nichos", "Tarifas", "Sepelios", "Vencimientos", "Movimientos", "Notificaciones", "Pagos"};
    private final String[] moduleDescriptions = {
        "Personas responsables y datos de contacto", "Registro de personas fallecidas", "Organización de sectores del cementerio",
        "Disponibilidad, ubicación y croquis", "Administración de costos por ubicación", "Programación y seguimiento de servicios",
        "Control de plazos y renovaciones", "Renovaciones, traslados y retiros", "Avisos y seguimiento de solicitudes", "Cobros y comprobantes registrados"
    };
    private final JLabel tableLabel = new JLabel("Registros");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new Main().setVisible(true);
        });
    }

    private Main() {
        super("Sistema de Gestión de Cementerio");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 650));
        setSize(1380, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());
        setIconImage(createAppIcon());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        status.setForeground(new Color(91, 106, 126));
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(227, 233, 242)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        add(status, BorderLayout.SOUTH);

        tab("Dolientes", "ID", "Nombre", "CI", "Teléfono", "Dirección");
        button("Dolientes", "Registrar doliente", this::addDoliente);
        tab("Difuntos", "ID", "Nombre", "CI", "Fallecimiento", "Doliente", "Observaciones");
        button("Difuntos", "Registrar difunto", this::addDifunto);
        button("Difuntos", "Buscar difunto", this::searchDifunto);
        tab("Pabellones", "ID", "Nombre", "Descripción");
        button("Pabellones", "Agregar pabellón", this::addPabellon);
        tab("Nichos", "ID", "Pabellón", "Nicho", "Nivel", "Ubicación", "Estado", "Tarifa (Bs)");
        button("Nichos", "Agregar nicho", this::addNicho);
        button("Nichos", "Ver disponibles", this::showAvailable);
        button("Nichos", "Ver croquis 10 x 10", this::showCroquis);
        tab("Tarifas", "ID", "Pabellón", "Posición", "Concepto", "Importe (Bs)", "Desde");
        button("Tarifas", "Modificar precios por fila", this::modificarPreciosFila);
        button("Tarifas", "Registrar tarifa", this::addTarifa);
        button("Tarifas", "Consultar costo de nicho", this::showCost);
        tab("Sepelios", "ID", "Difunto", "Fecha/hora", "Ubicación", "Responsable pago", "Sepulturero", "Vence", "Estado", "Costo (Bs)");
        button("Sepelios", "Programar sepelio", this::addSepelio);
        button("Sepelios", "Confirmar sepelio realizado", this::confirmarSepelio);
        button("Sepelios", "Cambiar responsable de pago", this::changeResponsible);
        button("Sepelios", "Buscar sepelio", this::searchSepelio);
        tab("Vencimientos", "Sepelio", "Difunto", "Ubicación", "Inicio", "Vencimiento", "Situación", "Días restantes");
        button("Vencimientos", "Próximos vencimientos", this::upcoming);
        tab("Movimientos", "ID", "Difunto", "Tipo", "Fecha", "Origen", "Destino", "Motivo");
        button("Movimientos", "Renovar 5 años", this::renew);
        button("Movimientos", "Trasladar / retirar", this::transfer);
        tab("Notificaciones", "ID", "Difunto", "Doliente", "Aviso", "Fecha", "Estado", "Motivo");
        button("Notificaciones", "Registrar aviso", this::addNotice);
        button("Notificaciones", "Marcar atendida", this::attendNotice);
        tab("Pagos", "ID", "Difunto", "Responsable", "Concepto", "Importe (Bs)", "Fecha", "Medio", "Referencia");
        button("Pagos", "Registrar pago", this::addPayment);
        button("Pagos", "Consultar pagos", this::queryPayments);
        button("Pagos", "Ver comprobante", this::verComprobante);
        button("Pagos", "Adjuntar comprobante", this::adjuntarComprobante);
        button("Pagos", "Copia de seguridad", this::copiaSeguridad);
        refresh();
        selectModule(0);
    }

    private Image createAppIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BLUE); g.fillRoundRect(1, 1, 62, 62, 17, 17);
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke(5));
        g.drawLine(32, 12, 32, 49); g.drawLine(19, 24, 45, 24);
        g.dispose(); return img;
    }

    private static class NavIcon implements Icon {
        final int index; final Color color;
        NavIcon(int index, Color color) { this.index=index; this.color=color; }
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D a=(Graphics2D)g.create();
            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            a.translate(x,y); a.setColor(color); a.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(index) {
                case 0: case 1:
                    a.drawOval(8,3,7,7); a.drawRoundRect(4,13,15,7,4,4); if(index==1) a.drawLine(11,1,11,3); break;
                case 2: a.drawRect(4,7,14,13); a.drawLine(2,7,11,2); a.drawLine(11,2,20,7); a.drawLine(9,20,9,13); a.drawLine(13,20,13,13); break;
                case 3: a.drawRoundRect(3,3,16,16,3,3); a.drawLine(11,3,11,19); a.drawLine(3,11,19,11); break;
                case 4: a.drawOval(4,4,14,14); a.drawString("$",8,15); break;
                case 5: a.drawRoundRect(3,5,16,15,3,3); a.drawLine(3,10,19,10); a.drawLine(7,2,7,7); a.drawLine(15,2,15,7); break;
                case 6: a.drawOval(4,4,14,14); a.drawLine(11,11,11,6); a.drawLine(11,11,15,13); break;
                case 7: a.drawLine(4,17,17,4); a.drawLine(12,4,17,4); a.drawLine(17,4,17,9); a.drawRoundRect(3,3,17,17,4,4); break;
                case 8: a.drawRoundRect(4,3,14,16,3,3); a.drawLine(7,8,15,8); a.drawLine(7,12,15,12); break;
                case 9: a.drawRoundRect(3,5,16,13,3,3); a.drawLine(3,9,19,9); a.drawLine(6,15,11,15); break;
            }
            a.dispose();
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(246, 0));
        sidebar.setBackground(NAVY);
        JPanel brand = new JPanel(new BorderLayout(12,0));
        brand.setBackground(NAVY);
        brand.setBorder(BorderFactory.createEmptyBorder(31, 20, 30, 15));
        JLabel logoMark = new JLabel("C", SwingConstants.CENTER);
        logoMark.setOpaque(true); logoMark.setBackground(BLUE); logoMark.setForeground(Color.WHITE);
        logoMark.setFont(new Font("Segoe UI", Font.BOLD, 23));
        logoMark.setPreferredSize(new Dimension(44,44));
        brand.add(logoMark,BorderLayout.WEST);
        JPanel brandText = new JPanel(); brandText.setOpaque(false); brandText.setLayout(new BoxLayout(brandText,BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("Cementerio");
        logo.setForeground(Color.WHITE); logo.setFont(new Font("Segoe UI", Font.BOLD, 19));
        JLabel sub = new JLabel("GESTIÓN MUNICIPAL");
        sub.setForeground(new Color(151, 175, 194)); sub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        brandText.add(logo); brandText.add(Box.createVerticalStrut(4)); brandText.add(sub);
        brand.add(brandText,BorderLayout.CENTER);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel(); menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(NAVY); menu.setBorder(BorderFactory.createEmptyBorder(4, 13, 10, 13));
        JLabel label = new JLabel("  MENÚ PRINCIPAL");
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(new Color(132, 156, 178)); label.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(label); menu.add(Box.createVerticalStrut(13));
        for (int i = 0; i < modules.length; i++) {
            final int index = i;
            String module = modules[i];
            JButton b = new JButton(module);
            b.setIcon(new NavIcon(i,new Color(192,211,226)));
            b.setIconTextGap(15);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            b.setForeground(new Color(219, 229, 238));
            b.setBackground(NAVY); b.setOpaque(true); b.setContentAreaFilled(true); b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 12));
            b.setMaximumSize(new Dimension(220, 43));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> selectModule(index));
            menuButtons.put(module, b);
            menu.add(b); menu.add(Box.createVerticalStrut(3));
        }
        JScrollPane scroller = new JScrollPane(menu);
        scroller.setBorder(null); scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getViewport().setBackground(NAVY);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scroller, BorderLayout.CENTER);
        JLabel footer = new JLabel("●  Datos guardados localmente", SwingConstants.CENTER);
        footer.setForeground(new Color(148, 177, 190));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(15, 5, 19, 5));
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BACKGROUND);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(228, 234, 242)),
            BorderFactory.createEmptyBorder(23, 28, 22, 28)));
        JPanel headings = new JPanel(); headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        headings.setOpaque(false);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 28)); sectionTitle.setForeground(INK);
        sectionInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12)); sectionInfo.setForeground(new Color(116, 129, 149));
        headings.add(sectionTitle); headings.add(Box.createVerticalStrut(5)); headings.add(sectionInfo);
        header.add(headings, BorderLayout.WEST);
        JLabel appTag = new JLabel("●  Sistema disponible");
        appTag.setForeground(new Color(30, 139, 102));
        appTag.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.add(appTag, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(0, 18));
        workspace.setBackground(BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel metrics = new JPanel(new GridLayout(1, 5, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Dolientes", counterDolientes, new Color(47, 100, 174)));
        metrics.add(metric("Difuntos", counterDifuntos, new Color(110, 92, 168)));
        metrics.add(metric("Nichos libres", counterNichos, new Color(20, 143, 119)));
        metrics.add(metric("Sepelios", counterSepelios, new Color(177, 120, 49)));
        metrics.add(metric("Por vencer", counterVencimientos, new Color(192, 86, 91)));
        workspace.add(metrics, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14)); body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JPanel bar = new JPanel(new BorderLayout()); bar.setBackground(Color.WHITE);
        tableLabel.setFont(new Font("Segoe UI", Font.BOLD, 16)); tableLabel.setForeground(INK);
        bar.add(tableLabel, BorderLayout.WEST);
        search.setToolTipText("Buscar por nombre, fecha o ubicación");
        search.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(212, 222, 235), 1, true),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });
        JPanel searchArea = new JPanel(new BorderLayout(10, 0)); searchArea.setOpaque(false);
        JLabel searchLabel = new JLabel("Buscar  "); searchLabel.setForeground(new Color(107, 121, 140));
        searchArea.add(searchLabel, BorderLayout.WEST); searchArea.add(search, BorderLayout.CENTER);
        bar.add(searchArea, BorderLayout.EAST);
        body.add(bar, BorderLayout.NORTH);
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override protected int calculateTabAreaHeight(int placement, int runs, int maxTabHeight) { return 0; }
            @Override protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) { }
        });
        tabs.setBorder(null); tabs.setBackground(Color.WHITE);
        body.add(tabs, BorderLayout.CENTER);
        workspace.add(body, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        return root;
    }

    private JPanel metric(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 234, 242), 1, true),
            BorderFactory.createEmptyBorder(19, 19, 19, 19)));
        JLabel label = new JLabel(title); label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(111, 125, 144));
        value.setFont(new Font("Segoe UI", Font.BOLD, 30)); value.setForeground(accent);
        card.add(label, BorderLayout.NORTH); card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void selectModule(int index) {
        tabs.setSelectedIndex(index);
        String current = modules[index];
        sectionTitle.setText(current);
        sectionInfo.setText(moduleDescriptions[index]);
        tableLabel.setText("Listado de " + current.toLowerCase(Locale.ROOT));
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            boolean active = entry.getKey().equals(current);
            JButton b = entry.getValue();
            b.setBackground(active ? new Color(33, 83, 89) : NAVY);
            b.setIcon(new NavIcon(java.util.Arrays.asList(modules).indexOf(entry.getKey()),active ? Color.WHITE : new Color(192,211,226)));
            b.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
            b.setForeground(Color.WHITE);
        }
        search.setText("");
        filterTable();
    }

    private void filterTable() {
        if (tabs.getTabCount() == 0) return;
        JTable table = grids.get(tabs.getTitleAt(tabs.getSelectedIndex()));
        if (table == null || !(table.getRowSorter() instanceof TableRowSorter)) return;
        @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> sorter =
            (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        String q = search.getText().trim();
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(q)));
    }

    private void tab(String title, String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(43);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(235, 239, 246));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(INK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(218, 239, 236));
        table.setSelectionForeground(INK);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 245, 247));
        table.getTableHeader().setForeground(new Color(77, 94, 118));
        table.getTableHeader().setPreferredSize(new Dimension(0, 47));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 9));
                if (!selected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 250, 251));
                    setForeground(INK);
                    String v = value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
                    if (v.equals("DISPONIBLE") || v.equals("VIGENTE") || v.equals("ATENDIDA"))
                        setForeground(new Color(18, 137, 97));
                    else if (v.equals("OCUPADO") || v.equals("VENCIDO") || v.equals("PENDIENTE"))
                        setForeground(new Color(201, 66, 76));
                    else if (v.equals("PRÓXIMO A VENCER") || v.equals("PROGRAMADO"))
                        setForeground(new Color(193, 122, 35));
                }
                return this;
            }
        });
        // Los identificadores permanecen en el modelo para las relaciones internas,
        // pero no se muestran en la interfaz.
        table.removeColumn(table.getColumnModel().getColumn(0));
        grids.put(title, table);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 7));
        actions.setBackground(Color.WHITE);
        actions.setName("actions");
        panel.add(actions, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(231, 236, 244)));
        scroll.getViewport().setBackground(Color.WHITE);
        scrolls.put(title, scroll);
        panel.add(scroll, BorderLayout.CENTER);
        panels.put(title, panel);
        tabs.addTab(title, panel);
    }

    private void button(String tab, String title, Runnable action) {
        JPanel p = (JPanel) panels.get(tab).getComponent(0);
        JButton b = new JButton(title) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(12, 94, 93) :
                    getModel().isRollover() ? new Color(17, 112, 110) : BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setForeground(Color.WHITE); b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            try { action.run(); }
            catch (Cancelled ignored) { }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });
        p.add(b);
    }

    private void commit(String message) {
        db.save();
        refresh();
        status.setText(" " + message + " | Guardado: " + LocalDateTime.now().withNano(0));
    }

    private void refresh() {
        fill("Dolientes", db.dolientes.values(), d -> row(d.id, d.nombre, d.ci, d.telefono, d.direccion));
        fill("Difuntos", db.difuntos.values(), d -> row(d.id, d.nombre, d.ci, d.fecha, doliente(d.dolienteId), d.observaciones));
        fill("Pabellones", db.pabellones.values(), p -> row(p.id, p.nombre, p.descripcion));
        fill("Nichos", db.nichos.values(), n -> row(n.id, pabellon(n.pabellonId), n.letra + n.fila, n.nivel, n.posicion, n.ocupado ? "OCUPADO" : "DISPONIBLE", money(tarifa(n))));
        fill("Tarifas", db.tarifas.values(), t -> row(t.id, (t.pabellonId == 0 ? "Todos los pabellones" : pabellon(t.pabellonId)), t.posicion, t.concepto, money(t.importe), t.desde));
        fill("Sepelios", db.sepelios.values(), s -> row(s.id, difunto(s.difuntoId), s.fechaHora, ubicacion(s), doliente(s.responsableId), s.sepulturero, s.vencimiento, s.estado, money(s.costo)));
        fill("Vencimientos", db.sepelios.values().stream().filter(s -> s.nichoId != 0).collect(Collectors.toList()), s -> row(s.id, difunto(s.difuntoId), ubicacion(s), s.inicio, s.vencimiento, situacion(s), Duration.between(LocalDate.now().atStartOfDay(), s.vencimiento.atStartOfDay()).toDays()));
        fill("Movimientos", db.movimientos.values(), m -> row(m.id, difuntoPorSepelio(m.sepelioId), m.tipo, m.fecha, m.origen, m.destino, m.motivo));
        fill("Notificaciones", db.notificaciones.values(), n -> row(n.id, difuntoPorSepelio(n.sepelioId), responsablePorSepelio(n.sepelioId), n.numero, n.fecha, n.estado, n.motivo));
        fill("Pagos", db.pagos.values(), p -> row(p.id, difuntoPorSepelio(p.sepelioId), doliente(p.responsableId), p.concepto, money(p.importe), p.fecha, p.medio, p.referencia));
        counterDolientes.setText(String.valueOf(db.dolientes.size()));
        counterDifuntos.setText(String.valueOf(db.difuntos.size()));
        counterNichos.setText(String.valueOf(db.nichos.values().stream().filter(n -> !n.ocupado).count()));
        counterSepelios.setText(String.valueOf(db.sepelios.size()));
        counterVencimientos.setText(String.valueOf(db.sepelios.values().stream().filter(s -> s.nichoId != 0 && !s.vencimiento.isAfter(LocalDate.now().plusDays(90))).count()));
    }

    private static Object[] row(Object... items) { return items; }
    private <T> void fill(String name, Collection<T> items, java.util.function.Function<T, Object[]> convert) {
        DefaultTableModel m = (DefaultTableModel) grids.get(name).getModel();
        m.setRowCount(0);
        for (T item : items) m.addRow(convert.apply(item));
        JScrollPane scroll = scrolls.get(name);
        if (scroll != null) {
            if (m.getRowCount() == 0) {
                JPanel empty = new JPanel(new GridBagLayout());
                empty.setBackground(Color.WHITE);
                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
                JLabel mark = new JLabel("○", SwingConstants.CENTER);
                mark.setFont(new Font("Segoe UI", Font.PLAIN, 46));
                mark.setForeground(new Color(157, 185, 188));
                mark.setAlignmentX(Component.CENTER_ALIGNMENT);
                JLabel headline = new JLabel("Todavía no hay registros");
                headline.setFont(new Font("Segoe UI", Font.BOLD, 18));
                headline.setForeground(INK);
                headline.setAlignmentX(Component.CENTER_ALIGNMENT);
                JLabel hint = new JLabel("Utiliza las acciones superiores para comenzar.");
                hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                hint.setForeground(new Color(126, 142, 154));
                hint.setAlignmentX(Component.CENTER_ALIGNMENT);
                text.add(mark); text.add(Box.createVerticalStrut(7)); text.add(headline);
                text.add(Box.createVerticalStrut(8)); text.add(hint);
                empty.add(text);
                scroll.setViewportView(empty);
            } else {
                scroll.setViewportView(grids.get(name));
            }
        }
    }
    private String doliente(int id) { Doliente d = db.dolientes.get(id); return d == null ? "Sin asignar" : d.nombre; }
    private String difunto(int id) { Difunto d = db.difuntos.get(id); return d == null ? "Sin asignar" : d.nombre; }
    private String difuntoPorSepelio(int id) { Sepelio s=db.sepelios.get(id); return s==null ? "Sin asignar" : difunto(s.difuntoId); }
    private String responsablePorSepelio(int id) { Sepelio s=db.sepelios.get(id); return s==null ? "Sin asignar" : doliente(s.responsableId); }
    private String pabellon(int id) { Pabellon p = db.pabellones.get(id); return p == null ? "Sin asignar" : p.nombre; }
    private String nicho(int id) { Nicho n = db.nichos.get(id); return n == null ? "Sin nicho" : pabellon(n.pabellonId) + " / " + n.fila + "-" + n.letra + " / " + n.nivel; }
    private String ubicacion(Sepelio s) { return s.nichoId == 0 ? s.estado : nicho(s.nichoId); }
    private static String money(BigDecimal v) { return v == null ? "Sin tarifa" : v.toPlainString(); }
    private String situacion(Sepelio s) {
        if (s.nichoId == 0) return "SIN NICHO";
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), s.vencimiento);
        return days < 0 ? "VENCIDO" : days <= 90 ? "PRÓXIMO A VENCER" : "VIGENTE";
    }
    private BigDecimal tarifa(Nicho n) {
        if (esNichoCroquis(n)) return precioFilaConfigurado(Integer.parseInt(n.fila), n.pabellonId);
        return db.tarifas.values().stream().filter(t -> t.pabellonId == n.pabellonId && t.posicion.equals(n.posicion) && !t.desde.isAfter(LocalDate.now()))
                .max(Comparator.comparing((Tarifa t) -> t.desde).thenComparingInt(t -> t.id)).map(t -> t.importe).orElse(null);
    }

    private String[] ask(String title, String[] labels, String[] defaults) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 17, 14, 17));
        JTextField[] inputs = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JLabel fieldLabel = new JLabel(labels[i]);
            fieldLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            fieldLabel.setForeground(INK);
            fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(fieldLabel); panel.add(Box.createVerticalStrut(7));
            inputs[i] = new JTextField(defaults == null ? "" : defaults[i], 30);
            inputs[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            inputs[i].setPreferredSize(new Dimension(400, 40));
            inputs[i].setMaximumSize(new Dimension(500, 40));
            inputs[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            inputs[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 219, 224), 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            panel.add(inputs[i]); panel.add(Box.createVerticalStrut(15));
        }
        JScrollPane pane = new JScrollPane(panel);
        pane.setBorder(null); pane.getViewport().setBackground(Color.WHITE);
        pane.setPreferredSize(new Dimension(460, Math.min(540, 36 + labels.length * 76)));
        int answer = JOptionPane.showConfirmDialog(this, pane, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) throw new Cancelled();
        String[] result = new String[labels.length];
        for (int i = 0; i < labels.length; i++) result[i] = inputs[i].getText().trim();
        return result;
    }
    private String[] ask(String title, String... labels) { return ask(title, labels, null); }
    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("El campo '" + name + "' es obligatorio.");
        return value.trim();
    }
    private static int positive(String value, String label) {
        try { int n = Integer.parseInt(required(value, label)); if (n > 0) return n; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + ": ingresa un número entero mayor a cero.");
    }
    private static LocalDate date(String value) {
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Fecha inválida. Usa AAAA-MM-DD."); }
    }
    private static LocalDateTime dateTime(String value) {
        try { return LocalDateTime.parse(value.replace(' ', 'T')); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Fecha/hora inválida. Usa AAAA-MM-DDTHH:MM."); }
    }
    private static BigDecimal amount(String value) {
        try { BigDecimal n = new BigDecimal(value.replace(',', '.')); if (n.signum() > 0 && n.scale() <= 2) return n; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Importe inválido. Debe ser mayor a cero y tener hasta dos decimales.");
    }
    private <T> T select(String title, Collection<T> values, java.util.function.Function<T, String> label) {
        if (values.isEmpty()) throw new IllegalArgumentException("No hay registros disponibles para: " + title);
        List<T> list = new ArrayList<>(values);
        JComboBox<String> combo = new JComboBox<>(list.stream().map(label).toArray(String[]::new));
        combo.setFont(new Font("Segoe UI",Font.PLAIN,14));
        combo.setPreferredSize(new Dimension(460,42));
        int response = JOptionPane.showConfirmDialog(this, combo, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response != JOptionPane.OK_OPTION) throw new Cancelled();
        return list.get(combo.getSelectedIndex());
    }
    private String choose(String title, String... choices) {
        String value = (String) JOptionPane.showInputDialog(this, title, title, JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
        if (value == null) throw new Cancelled();
        return value;
    }
    private void info(String text) { JOptionPane.showMessageDialog(this, text, "Información", JOptionPane.INFORMATION_MESSAGE); }

    private void addDoliente() {
        String[] v = ask("Registrar doliente", "Nombre completo", "CI", "Teléfono", "Dirección");
        required(v[0], "Nombre completo"); required(v[1], "CI");
        if (db.dolientes.values().stream().anyMatch(d -> d.ci.equalsIgnoreCase(v[1]))) throw new IllegalArgumentException("Ya existe un doliente con ese CI.");
        Doliente d = new Doliente(); d.id = db.next(); d.nombre = v[0]; d.ci = v[1]; d.telefono = v[2]; d.direccion = v[3];
        db.dolientes.put(d.id, d); commit("Doliente registrado");
    }
    private void addDifunto() {
        Doliente d = select("Selecciona el doliente", db.dolientes.values(), x -> x.nombre + " / " + x.ci);
        String[] v = ask("Registrar difunto", "Nombre completo", "CI (opcional)", "Fecha de fallecimiento (AAAA-MM-DD)", "Observaciones");
        required(v[0], "Nombre completo"); LocalDate fallecimiento = date(v[2]);
        if (fallecimiento.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de fallecimiento no puede ser futura.");
        if (!v[1].trim().isEmpty() && db.difuntos.values().stream().anyMatch(x -> x.ci.equalsIgnoreCase(v[1]))) throw new IllegalArgumentException("Ya existe un difunto con ese CI.");
        Difunto f = new Difunto(); f.id = db.next(); f.nombre = v[0]; f.ci = v[1]; f.fecha = fallecimiento; f.dolienteId = d.id; f.observaciones = v[3];
        db.difuntos.put(f.id, f); commit("Difunto registrado");
    }
    private void searchDifunto() {
        String[] v = ask("Buscar difunto", "Nombre o CI (puede ser parcial)");
        String q = required(v[0], "Búsqueda").toLowerCase(Locale.ROOT);
        List<Difunto> matches = db.difuntos.values().stream()
            .filter(d -> d.nombre.toLowerCase(Locale.ROOT).contains(q) || d.ci.toLowerCase(Locale.ROOT).contains(q))
            .collect(Collectors.toList());
        if (matches.isEmpty()) { info("No se encontraron difuntos."); return; }
        List<Object[]> rows = new ArrayList<>();
        for (Difunto d : matches) {
            List<Sepelio> ss = db.sepelios.values().stream().filter(x -> x.difuntoId == d.id).collect(Collectors.toList());
            if (ss.isEmpty()) {
                rows.add(new Object[]{d.nombre, d.ci, doliente(d.dolienteId), "Sin sepelio", "Sin asignar", "Pendiente"});
            } else {
                for (Sepelio se : ss) rows.add(new Object[]{d.nombre, d.ci, doliente(d.dolienteId), "Sepelio registrado", ubicacion(se), situacion(se)});
            }
        }
        showResults("Resultados de búsqueda", matches.size() + " persona(s) encontrada(s)",
            new String[]{"Difunto", "CI", "Doliente", "Registro", "Ubicación", "Estado"}, rows);
    }
    private void addPabellon() {
        String[] v = ask("Registrar pabellón", "Nombre", "Descripción");
        required(v[0], "Nombre");
        if (db.pabellones.values().stream().anyMatch(p -> p.nombre.equalsIgnoreCase(v[0]))) throw new IllegalArgumentException("Ya existe ese pabellón.");
        Pabellon p = new Pabellon(); p.id = db.next(); p.nombre = v[0]; p.descripcion = v[1];
        db.pabellones.put(p.id, p); crearNichosCroquis(p); commit("Pabellón registrado" + " con 100 nichos");
    }
    private void addNicho() {
        Pabellon p = select("Selecciona pabellón", db.pabellones.values(), x -> x.nombre);
        String[] v = ask("Registrar nicho", "Fila", "Letra", "Nivel (entero)");
        required(v[0], "Fila"); required(v[1], "Letra"); int nivel = positive(v[2], "Nivel");
        String posicion = choose("Posición para tarifa", "INFERIOR", "MEDIA", "SUPERIOR");
        if (db.nichos.values().stream().anyMatch(n -> n.pabellonId == p.id && n.fila.equalsIgnoreCase(v[0]) && n.letra.equalsIgnoreCase(v[1]) && n.nivel == nivel)) throw new IllegalArgumentException("Ya existe ese nicho en el pabellón.");
        Nicho n = new Nicho(); n.id = db.next(); n.pabellonId = p.id; n.fila = v[0]; n.letra = v[1].toUpperCase(Locale.ROOT); n.nivel = nivel; n.posicion = posicion;
        db.nichos.put(n.id, n); commit("Nicho registrado");
    }
    // Filas 1 y 2 son las inferiores; 9 y 10 son las superiores.
    private static BigDecimal precioFila(int fila) {
        return BigDecimal.valueOf(1000 - ((fila - 1) / 2) * 100);
    }
    // Las tarifas del croquis se guardan como registros FILA_1 ... FILA_10.
    // Una tarifa global (pabellonId=0) se aplica a todos los pabellones.
    private BigDecimal precioFilaConfigurado(int fila, int pabellonId) {
        String clave = "FILA_" + fila;
        return db.tarifas.values().stream()
            .filter(t -> t.posicion.equals(clave) && (t.pabellonId == 0 || t.pabellonId == pabellonId)
                && !t.desde.isAfter(LocalDate.now()))
            .max(Comparator.comparingInt((Tarifa t) -> t.pabellonId == pabellonId ? 1 : 0)
                .thenComparing(t -> t.desde).thenComparingInt(t -> t.id))
            .map(t -> t.importe).orElse(precioFila(fila));
    }

    private void modificarPreciosFila() {
        String grupo = choose("Filas cuyos precios deseas cambiar",
            "1 y 2 (inferiores)", "3 y 4", "5 y 6", "7 y 8", "9 y 10 (superiores)");
        int grupoIndex = java.util.Arrays.asList("1 y 2 (inferiores)", "3 y 4", "5 y 6", "7 y 8", "9 y 10 (superiores)").indexOf(grupo);
        int primeraFila = grupoIndex * 2 + 1;
        BigDecimal actual = precioFilaConfigurado(primeraFila, 0);
        String[] datos = ask("Modificar precio de filas " + primeraFila + " y " + (primeraFila + 1),
            new String[]{"Nuevo precio (Bs)"}, new String[]{money(actual)});
        BigDecimal nuevo = amount(datos[0]);
        if (nuevo.signum() <= 0) throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        for (int fila = primeraFila; fila <= primeraFila + 1; fila++) {
            final String clave = "FILA_" + fila;
            db.tarifas.values().removeIf(t -> t.pabellonId == 0 && clave.equals(t.posicion));
            Tarifa t = new Tarifa();
            t.id = db.next(); t.pabellonId = 0; t.posicion = clave;
            t.concepto = "Precio de nicho fila " + fila;
            t.importe = nuevo; t.desde = LocalDate.now();
            db.tarifas.put(t.id, t);
        }
        commit("Precio actualizado: filas " + primeraFila + "-" + (primeraFila + 1) + " = Bs " + money(nuevo));
    }

    private static boolean esNichoCroquis(Nicho n) {
        try {
            int f = Integer.parseInt(n.fila);
            return f >= 1 && f <= 10 && n.letra != null && n.letra.length() == 1
                && n.letra.charAt(0) >= 'A' && n.letra.charAt(0) <= 'J'
                && n.nivel == f;
        } catch (NumberFormatException ex) { return false; }
    }
    private void crearNichosCroquis(Pabellon p) {
        for (int fila = 1; fila <= 10; fila++) {
            for (int col = 0; col < 10; col++) {
                final int f = fila;
                final String letra = String.valueOf((char) ('A' + col));
                boolean existe = db.nichos.values().stream().anyMatch(n -> n.pabellonId == p.id
                    && n.fila.equals(String.valueOf(f)) && n.letra.equalsIgnoreCase(letra));
                if (existe) continue;
                Nicho n = new Nicho(); n.id = db.next(); n.pabellonId = p.id;
                n.fila = String.valueOf(fila); n.letra = letra; n.nivel = fila;
                n.posicion = fila <= 2 ? "INFERIOR" : fila <= 8 ? "MEDIA" : "SUPERIOR";
                db.nichos.put(n.id, n);
            }
        }
    }
    private void showCroquis() {
        Pabellon p = select("Selecciona el pabellón", db.pabellones.values(), x -> x.nombre);
        crearNichosCroquis(p);
        db.save(); refresh();
        JDialog dialog = new JDialog(this, "Croquis de nichos - " + p.nombre, true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        JLabel title = new JLabel(p.nombre.toUpperCase(Locale.ROOT));
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(INK);
        top.add(title, BorderLayout.NORTH);
        JLabel legend = new JLabel("● VERDE: DISPONIBLE       ● ROJO: OCUPADO       |       Arriba: fila 10   /   Abajo: fila 1 (más cara)");
        legend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        legend.setForeground(new Color(42, 57, 76));
        top.add(legend, BorderLayout.SOUTH);
        dialog.add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        dialog.add(grid, BorderLayout.CENTER);
        JLabel footer = new JLabel("Columnas A-J  |  Filas 1-10  |  Clic en verde: registrar sepelio  |  Clic en rojo: consultar datos", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footer.setForeground(INK);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 5, 18, 5));
        dialog.add(footer, BorderLayout.SOUTH);
        redrawCroquis(grid, dialog, p);
        dialog.setSize(1150, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel etiquetaCoordenada(String value) {
        JLabel label = new JLabel(value, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(99, 115, 134));
        label.setOpaque(true);
        label.setBackground(new Color(242, 246, 250));
        label.setBorder(BorderFactory.createLineBorder(new Color(229, 236, 244)));
        label.setPreferredSize(new Dimension(30, 30));
        return label;
    }

    private void addCroquisItem(JPanel grid, Component component, int column, int row, boolean header) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.weightx = header && (column == 0 || column == 11) ? 0.05 : 1.0;
        c.weighty = header && (row == 0 || row == 11) ? 0.06 : 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        grid.add(component, c);
    }

    private void redrawCroquis(JPanel grid, JDialog dialog, Pabellon p) {
        grid.removeAll();
        addCroquisItem(grid, etiquetaCoordenada(""), 0, 0, true);
        for (int col = 0; col < 10; col++) addCroquisItem(grid, etiquetaCoordenada(String.valueOf((char) ('A' + col))), col + 1, 0, true);
        addCroquisItem(grid, etiquetaCoordenada(""), 11, 0, true);
        for (int fila = 10; fila >= 1; fila--) {
            final int f = fila;
            int row = 11 - fila;
            addCroquisItem(grid, etiquetaCoordenada(String.valueOf(f)), 0, row, true);
            for (int col = 0; col < 10; col++) {
                final String letra = String.valueOf((char) ('A' + col));
                Nicho n = db.nichos.values().stream().filter(x -> x.pabellonId == p.id
                    && x.fila.equals(String.valueOf(f)) && x.letra.equalsIgnoreCase(letra)).findFirst().orElse(null);
                BigDecimal precio = precioFilaConfigurado(f, p.id);
                JButton cell = new JButton("<html><center><b>" + letra + f + "</b><br><small>Bs " + money(precio) + "</small></center></html>");
                cell.setUI(new javax.swing.plaf.basic.BasicButtonUI());
                cell.setFont(new Font("Segoe UI", Font.BOLD, 12));
                cell.setForeground(Color.WHITE);
                cell.setBackground(n != null && n.ocupado ? new Color(181, 54, 66) : new Color(18, 133, 91));
                cell.setOpaque(true); cell.setContentAreaFilled(true);
                cell.setFocusPainted(false);
                cell.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 1));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cell.setToolTipText("Nicho " + letra + f + " | Fila " + f + " | Bs " + money(precio)
                    + " | " + (n != null && n.ocupado ? "OCUPADO" : "DISPONIBLE"));
                cell.addActionListener(e -> {
                    try {
                        if (n == null) throw new IllegalArgumentException("Nicho no encontrado.");
                        if (n.ocupado) {
                            Sepelio sepelio = db.sepelios.values().stream()
                                .filter(x -> x.nichoId == n.id).findFirst().orElse(null);
                            info("Nicho " + letra + f + " - OCUPADO\n" + (sepelio == null ? "Sin sepelio vinculado." :
                                "Difunto: " + difunto(sepelio.difuntoId) +
                                "\nResponsable: " + doliente(sepelio.responsableId) +
                                "\nFecha: " + sepelio.fechaHora +
                                "\nVencimiento: " + sepelio.vencimiento +
                                "\nCosto registrado: Bs " + money(sepelio.costo)));
                        } else registrarSepelioEnNicho(n);
                        redrawCroquis(grid, dialog, p);
                    } catch (Cancelled ignored) { }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                addCroquisItem(grid, cell, col + 1, row, false);
            }
            addCroquisItem(grid, etiquetaCoordenada(String.valueOf(f)), 11, row, true);
        }
        addCroquisItem(grid, etiquetaCoordenada(""), 0, 11, true);
        for (int col = 0; col < 10; col++) addCroquisItem(grid, etiquetaCoordenada(String.valueOf((char) ('A' + col))), col + 1, 11, true);
        addCroquisItem(grid, etiquetaCoordenada(""), 11, 11, true);
        grid.revalidate();
        grid.repaint();
    }
    private void registrarSepelioEnNicho(Nicho n) {
        if (n.ocupado) throw new IllegalArgumentException("Este nicho ya está ocupado.");
        Set<Integer> assigned = db.sepelios.values().stream().map(s -> s.difuntoId).collect(Collectors.toSet());
        Difunto d = select("Difunto para el nicho " + n.fila + n.letra,
            db.difuntos.values().stream().filter(x -> !assigned.contains(x.id)).collect(Collectors.toList()), x -> x.nombre);
        Doliente r = select("Responsable de pago", db.dolientes.values(), x -> x.nombre + " / " + x.ci);
        String[] v = ask("Registrar sepelio - Nicho " + n.fila + n.letra + " (Bs " + money(tarifa(n)) + ")",
            new String[]{"Fecha y hora (AAAA-MM-DDTHH:MM)", "Sepulturero asignado", "Inicio de ocupación (AAAA-MM-DD)"},
            new String[]{LocalDateTime.now().withSecond(0).withNano(0).toString(), "", LocalDate.now().toString()});
        LocalDateTime fecha = dateTime(v[0]); required(v[1], "Sepulturero"); LocalDate inicio = date(v[2]);
        if (inicio.isBefore(fecha.toLocalDate())) throw new IllegalArgumentException("Inicio anterior a fecha del sepelio.");
        Sepelio s = new Sepelio(); s.id = db.next(); s.difuntoId = d.id; s.nichoId = n.id;
        s.responsableId = r.id; s.fechaHora = fecha; s.sepulturero = v[1]; s.inicio = inicio;
        s.vencimiento = inicio.plusYears(5); s.estado = "PROGRAMADO"; s.costo = tarifa(n);
        db.sepelios.put(s.id, s); n.ocupado = true;
        commit("Sepelio registrado; nicho " + n.fila + n.letra + " ocupado");
    }
    private void showAvailable() {
        String s = db.nichos.values().stream().filter(n -> !n.ocupado).map(n -> nicho(n.id) + " | Tarifa: Bs " + money(tarifa(n))).collect(Collectors.joining("\n"));
        showText("Nichos disponibles", s.trim().isEmpty() ? "No hay nichos disponibles." : s);
    }
    private void addTarifa() {
        Pabellon p = select("Selecciona pabellón", db.pabellones.values(), x -> x.nombre);
        String posicion = choose("Posición", "INFERIOR", "MEDIA", "SUPERIOR");
        String[] v = ask("Registrar tarifa", new String[]{"Concepto", "Importe en Bs", "Vigente desde (AAAA-MM-DD)"}, new String[]{"Ocupación de nicho", "", LocalDate.now().toString()});
        required(v[0], "Concepto"); BigDecimal importe = amount(v[1]); LocalDate desde = date(v[2]);
        Tarifa t = new Tarifa(); t.id = db.next(); t.pabellonId = p.id; t.posicion = posicion; t.concepto = v[0]; t.importe = importe; t.desde = desde;
        db.tarifas.put(t.id, t); commit("Tarifa registrada");
    }
    private void showCost() {
        Nicho n = select("Selecciona nicho", db.nichos.values(), x -> nicho(x.id));
        info("Nicho: " + nicho(n.id) + "\nPosición: " + n.posicion + "\nCosto: Bs " + money(tarifa(n)) + "\nEstado: " + (n.ocupado ? "OCUPADO" : "DISPONIBLE"));
    }
    private void addSepelio() {
        Set<Integer> assigned = db.sepelios.values().stream().map(s -> s.difuntoId).collect(Collectors.toSet());
        Difunto d = select("Selecciona difunto sin sepelio", db.difuntos.values().stream().filter(x -> !assigned.contains(x.id)).collect(Collectors.toList()), x -> x.nombre);
        Nicho n = select("Selecciona nicho disponible", db.nichos.values().stream().filter(x -> !x.ocupado).collect(Collectors.toList()), x -> nicho(x.id));
        BigDecimal cost = tarifa(n);
        if (cost == null) throw new IllegalArgumentException("Debes registrar una tarifa vigente para la posición y pabellón del nicho.");
        Doliente r = select("Responsable de pago", db.dolientes.values(), x -> x.nombre + " / " + x.ci);
        String[] v = ask("Programar sepelio", new String[]{"Fecha y hora (AAAA-MM-DDTHH:MM)", "Sepulturero asignado", "Inicio de ocupación (AAAA-MM-DD)"}, new String[]{LocalDateTime.now().withSecond(0).withNano(0).toString(), "", LocalDate.now().toString()});
        LocalDateTime fecha = dateTime(v[0]); required(v[1], "Sepulturero"); LocalDate inicio = date(v[2]);
        if (inicio.isBefore(fecha.toLocalDate())) throw new IllegalArgumentException("El inicio de ocupación no puede ser anterior a la fecha del sepelio.");
        Sepelio s = new Sepelio(); s.id = db.next(); s.difuntoId = d.id; s.nichoId = n.id; s.responsableId = r.id; s.fechaHora = fecha; s.sepulturero = v[1]; s.inicio = inicio; s.vencimiento = inicio.plusYears(5); s.estado = "PROGRAMADO"; s.costo = cost;
        db.sepelios.put(s.id, s); n.ocupado = true; commit("Sepelio programado" + " | Vence " + s.vencimiento);
    }
    private void changeResponsible() {
        Sepelio s = select("Selecciona sepelio", db.sepelios.values(), x -> difunto(x.difuntoId));
        Doliente d = select("Nuevo responsable de pago", db.dolientes.values(), x -> x.nombre + " / " + x.ci);
        s.responsableId = d.id; commit("Responsable actualizado en sepelio");
    }
    private void searchSepelio() {
        String[] v = ask("Buscar sepelio", "Nombre de difunto o número de sepelio");
        String q = required(v[0], "Búsqueda").toLowerCase(Locale.ROOT);
        List<Sepelio> matches = db.sepelios.values().stream()
            .filter(x -> ("" + x.id).equals(q) || difunto(x.difuntoId).toLowerCase(Locale.ROOT).contains(q) || (db.difuntos.get(x.difuntoId) != null && db.difuntos.get(x.difuntoId).ci != null && db.difuntos.get(x.difuntoId).ci.toLowerCase(Locale.ROOT).contains(q)))
            .collect(Collectors.toList());
        if (matches.isEmpty()) { info("No se encontraron sepelios."); return; }
        List<Object[]> rows = new ArrayList<>();
        for (Sepelio se : matches) rows.add(new Object[]{difunto(se.difuntoId), ubicacion(se),
            String.valueOf(se.fechaHora), doliente(se.responsableId), situacion(se)});
        showResults("Sepelios encontrados", matches.size() + " sepelio(s) encontrado(s)",
            new String[]{"Difunto", "Ubicación", "Fecha y hora", "Responsable", "Estado"}, rows);
    }
    private void upcoming() {
        String[] v = ask("Consultar vencimientos", new String[]{"Próximos días (incluye vencidos)"}, new String[]{"90"});
        int days = positive(v[0], "Días");
        String s = db.sepelios.values().stream().filter(x -> x.nichoId != 0 && !x.vencimiento.isAfter(LocalDate.now().plusDays(days)))
                .sorted(Comparator.comparing(x -> x.vencimiento))
                .map(x -> "Sepelio " + x.id + " | " + difunto(x.difuntoId) + " | Vence: " + x.vencimiento + " | " + situacion(x)).collect(Collectors.joining("\n"));
        showText("Próximos vencimientos", s.trim().isEmpty() ? "No hay vencimientos en el plazo consultado." : s);
    }
    private void renew() {
        Sepelio s = select("Sepelio a renovar", db.sepelios.values().stream().filter(x -> x.nichoId != 0).collect(Collectors.toList()), x -> difunto(x.difuntoId) + " / vence " + x.vencimiento);
        String[] v = ask("Renovación", "Motivo / observación");
        LocalDate old = s.vencimiento;
        LocalDate base = old.isAfter(LocalDate.now()) ? old : LocalDate.now();
        s.vencimiento = base.plusYears(5);
        Movimiento m = movement(s.id, "RENOVACIÓN", nicho(s.nichoId), nicho(s.nichoId), v[0]);
        db.movimientos.put(m.id, m); commit("Renovación registrada. Nueva fecha: " + s.vencimiento);
    }
    private void transfer() {
        Sepelio s = select("Sepelio a trasladar", db.sepelios.values().stream().filter(x -> x.nichoId != 0).collect(Collectors.toList()), x -> difunto(x.difuntoId));
        String tipo = choose("Destino del movimiento", "OTRO NICHO", "BODEGA", "OSARIO", "CREMACIÓN");
        Nicho destination = null;
        if (tipo.equals("OTRO NICHO")) destination = select("Nicho de destino", db.nichos.values().stream().filter(x -> !x.ocupado).collect(Collectors.toList()), x -> nicho(x.id));
        String[] v = ask("Registrar movimiento", "Motivo del traslado / retiro");
        String origen = nicho(s.nichoId); String destino = destination == null ? tipo : nicho(destination.id);
        db.nichos.get(s.nichoId).ocupado = false;
        if (destination == null) { s.nichoId = 0; s.estado = tipo; }
        else { destination.ocupado = true; s.nichoId = destination.id; s.estado = "TRASLADADO"; s.inicio = LocalDate.now(); s.vencimiento = s.inicio.plusYears(5); BigDecimal cost = tarifa(destination); if (cost != null) s.costo = cost; }
        Movimiento m = movement(s.id, "TRASLADO", origen, destino, v[0]);
        db.movimientos.put(m.id, m); commit("Movimiento registrado");
    }
    private Movimiento movement(int sepelio, String tipo, String origen, String destino, String motivo) {
        Movimiento m = new Movimiento(); m.id = db.next(); m.sepelioId = sepelio; m.tipo = tipo; m.fecha = LocalDate.now(); m.origen = origen; m.destino = destino; m.motivo = motivo; return m;
    }
    private void addNotice() {
        Sepelio s = select("Selecciona sepelio", db.sepelios.values().stream().filter(x -> x.nichoId != 0).collect(Collectors.toList()), x -> difunto(x.difuntoId) + " / " + situacion(x));
        String numero = choose("Número de notificación", "PRIMERA", "SEGUNDA");
        if (db.notificaciones.values().stream().anyMatch(n -> n.sepelioId == s.id && n.numero.equals(numero))) throw new IllegalArgumentException("Ya se registró esa notificación para este sepelio.");
        if (numero.equals("SEGUNDA") && db.notificaciones.values().stream().noneMatch(n -> n.sepelioId == s.id && n.numero.equals("PRIMERA"))) throw new IllegalArgumentException("Primero debes registrar la primera notificación.");
        String[] v = ask("Registrar notificación", "Motivo del aviso");
        Notificacion n = new Notificacion(); n.id = db.next(); n.sepelioId = s.id; n.numero = numero; n.fecha = LocalDate.now(); n.estado = "PENDIENTE"; n.motivo = required(v[0], "Motivo");
        db.notificaciones.put(n.id, n); commit("Notificación registrada");
    }
    private void attendNotice() {
        Notificacion n = select("Notificación pendiente", db.notificaciones.values().stream().filter(x -> x.estado.equals("PENDIENTE")).collect(Collectors.toList()), x -> difuntoPorSepelio(x.sepelioId) + " | " + x.numero);
        n.estado = "ATENDIDA"; commit("Notificación atendida");
    }
    private void addPayment() {
        Sepelio s = select("Selecciona sepelio", db.sepelios.values(), x -> difunto(x.difuntoId));
        String[] v = ask("Registrar pago", new String[]{"Concepto", "Monto (Bs)", "Medio (EFECTIVO / QR EXTERNO / TRANSFERENCIA)", "Referencia / comprobante"}, new String[]{"Servicio funerario", "", "EFECTIVO", ""});
        required(v[0], "Concepto"); BigDecimal importe = amount(v[1]);
        String medio = required(v[2], "Medio").toUpperCase(Locale.ROOT);
        if (!Arrays.asList("EFECTIVO", "QR EXTERNO", "TRANSFERENCIA").contains(medio)) throw new IllegalArgumentException("Medio permitido: EFECTIVO, QR EXTERNO o TRANSFERENCIA.");
        if (!medio.equals("EFECTIVO")) required(v[3], "Referencia");
        Pago p = new Pago(); p.id = db.next(); p.sepelioId = s.id; p.responsableId = s.responsableId; p.concepto = v[0]; p.importe = importe; p.fecha = LocalDate.now(); p.medio = medio; p.referencia = v[3];
        db.pagos.put(p.id, p); commit("Pago registrado");
    }
    private void queryPayments() {
        Sepelio s = select("Pagos del sepelio", db.sepelios.values(), x -> difunto(x.difuntoId));
        List<Pago> payments = db.pagos.values().stream().filter(p -> p.sepelioId == s.id).collect(Collectors.toList());
        BigDecimal total = payments.stream().map(p -> p.importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        String detail = payments.stream().map(p -> p.fecha + " | " + p.concepto + " | Bs " + money(p.importe) + " | " + p.medio).collect(Collectors.joining("\n"));
        showText("Historial de pagos", "Sepelio de " + difunto(s.difuntoId) + "\nCosto registrado: Bs " + money(s.costo) + "\nTotal pagado: Bs " + money(total) + "\nSaldo orientativo: Bs " + money(s.costo.subtract(total)) + "\n\n" + (detail.trim().isEmpty() ? "Sin pagos registrados." : detail));
    }
    // HU6: confirmación de ejecución por el personal encargado.
    private void confirmarSepelio() {
        Sepelio s = select("Sepelio pendiente de ejecución",
            db.sepelios.values().stream()
                .filter(x -> "PROGRAMADO".equals(x.estado))
                .collect(Collectors.toList()),
            x -> difunto(x.difuntoId) + " | " + x.fechaHora + " | " + x.sepulturero);
        if (JOptionPane.showConfirmDialog(this,
                "¿Confirmar que el sepulturero " + s.sepulturero
                + " realizó el sepelio de " + difunto(s.difuntoId) + "?",
                "Confirmación de ejecución", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        s.estado = "REALIZADO";
        commit("Sepelio confirmado como realizado");
    }

    // HU5: comprobante de pago externo asociado a un pago registrado.
    private void adjuntarComprobante() {
        Pago p = select("Selecciona el pago",
            db.pagos.values(),
            x -> difuntoPorSepelio(x.sepelioId) + " | Bs " + money(x.importe)
                + " | " + x.fecha + " | " + x.medio);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar comprobante de pago");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes o PDF", "png", "jpg", "jpeg", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path original = chooser.getSelectedFile().toPath();
        String fileName = original.getFileName().toString();
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Arrays.asList("png", "jpg", "jpeg", "pdf").contains(ext))
            throw new IllegalArgumentException("Solo se permiten archivos PNG, JPG o PDF.");
        try {
            if (Files.size(original) > 10L * 1024L * 1024L)
                throw new IllegalArgumentException("El comprobante debe pesar como máximo 10 MB.");
            Path dir = Data.FILE.getParent().resolve("comprobantes");
            Files.createDirectories(dir);
            Path target = dir.resolve("pago_" + p.id + "." + ext);
            if (p.comprobante != null && !p.comprobante.isEmpty()) {
                Path old = dir.resolve(p.comprobante);
                if (!old.equals(target)) Files.deleteIfExists(old);
            }
            Files.copy(original, target, StandardCopyOption.REPLACE_EXISTING);
            p.comprobante = target.getFileName().toString();
            commit("Comprobante asociado al pago");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo adjuntar el comprobante: " + ex.getMessage(), ex);
        }
    }

    private void verComprobante() {
        Pago p = select("Selecciona el pago",
            db.pagos.values().stream().filter(x -> x.comprobante != null && !x.comprobante.isEmpty())
                .collect(Collectors.toList()),
            x -> difuntoPorSepelio(x.sepelioId) + " | Bs " + money(x.importe)
                + " | " + x.fecha);
        Path path = Data.FILE.getParent().resolve("comprobantes").resolve(p.comprobante);
        if (!Files.isRegularFile(path)) {
            info("No se encontró el archivo adjunto. Verifica la copia de seguridad.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
            else info("Comprobante guardado en: " + path);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo abrir el comprobante: " + ex.getMessage(), ex);
        }
    }

    // Respaldo de datos y comprobantes para trasladar el sistema a otro equipo.
    private void copiaSeguridad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar copia de seguridad ZIP");
        chooser.setSelectedFile(new File("Cementerio_Respaldo_" + LocalDate.now() + ".zip"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path dest = chooser.getSelectedFile().toPath();
        if (!dest.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
            dest = dest.resolveSibling(dest.getFileName().toString() + ".zip");
        db.save();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(dest))) {
            agregarAlZip(zip, Data.FILE, "datos.ser");
            Path dir = Data.FILE.getParent().resolve("comprobantes");
            if (Files.isDirectory(dir)) {
                try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                    for (Path p : files.filter(Files::isRegularFile).collect(Collectors.toList()))
                        agregarAlZip(zip, p, "comprobantes/" + p.getFileName());
                }
            }
            info("Copia de seguridad creada correctamente:\\n" + dest);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el respaldo: " + ex.getMessage(), ex);
        }
    }

    private static void agregarAlZip(java.util.zip.ZipOutputStream zip, Path file, String entry) throws IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(entry));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private void showResults(String title, String summary, String[] columns, List<Object[]> rows) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout(0, 18));
        dialog.getContentPane().setBackground(new Color(247, 249, 252));
        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(22, 26, 0, 26));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 21));
        heading.setForeground(INK);
        JLabel sub = new JLabel(summary);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(102, 116, 131));
        head.add(heading, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        dialog.add(head, BorderLayout.NORTH);
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        for (Object[] row : rows) model.addRow(row);
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(43);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(232, 237, 243));
        table.setSelectionBackground(new Color(218, 238, 237));
        table.setSelectionForeground(INK);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(10, 40));
        table.getTableHeader().setBackground(new Color(230, 237, 246));
        table.getTableHeader().setForeground(INK);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(224, 232, 240)));
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 26));
        center.add(scroll, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
        JButton close = new JButton("Cerrar");
        close.setFont(new Font("Segoe UI", Font.BOLD, 13));
        close.setBackground(BLUE);
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.setPreferredSize(new Dimension(110, 36));
        close.addActionListener(e -> dialog.dispose());
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(1040, Math.min(650, Math.max(330, 190 + rows.size() * 45)));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showText(String title, String text) {
        JTextArea area = new JTextArea(text, 16, 68);
        area.setEditable(false); area.setCaretPosition(0);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBackground(Color.WHITE); area.setForeground(INK);
        area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(710, 420));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(225, 232, 236)));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.PLAIN_MESSAGE);
    }

    private static final class Cancelled extends RuntimeException { private static final long serialVersionUID = 1L; }

    private static final class Data implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final Path FILE = Paths.get(System.getProperty("user.home"), ".cementerio_municipal", "datos.ser");
        private int seq = 0;
        final Map<Integer, Doliente> dolientes = new LinkedHashMap<>();
        final Map<Integer, Difunto> difuntos = new LinkedHashMap<>();
        final Map<Integer, Pabellon> pabellones = new LinkedHashMap<>();
        final Map<Integer, Nicho> nichos = new LinkedHashMap<>();
        final Map<Integer, Tarifa> tarifas = new LinkedHashMap<>();
        final Map<Integer, Sepelio> sepelios = new LinkedHashMap<>();
        final Map<Integer, Movimiento> movimientos = new LinkedHashMap<>();
        final Map<Integer, Notificacion> notificaciones = new LinkedHashMap<>();
        final Map<Integer, Pago> pagos = new LinkedHashMap<>();
        int next() { return ++seq; }
        static Data load() {
            if (!Files.exists(FILE)) return new Data();
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) { return (Data) in.readObject(); }
            catch (Exception ex) { throw new IllegalStateException("No se pudo leer la base local: " + FILE + "\n" + ex.getMessage(), ex); }
        }
        void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Path tmp = FILE.resolveSibling("datos.tmp");
                try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) { out.writeObject(this); }
                try { Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING); }
            } catch (IOException ex) { throw new IllegalStateException("No se pudieron guardar los datos: " + ex.getMessage(), ex); }
        }
    }
    private static final class Doliente implements Serializable { private static final long serialVersionUID=1L; int id; String nombre, ci, telefono, direccion; }
    private static final class Difunto implements Serializable { private static final long serialVersionUID=1L; int id, dolienteId; String nombre, ci, observaciones; LocalDate fecha; }
    private static final class Pabellon implements Serializable { private static final long serialVersionUID=1L; int id; String nombre, descripcion; }
    private static final class Nicho implements Serializable { private static final long serialVersionUID=1L; int id, pabellonId, nivel; String fila, letra, posicion; boolean ocupado; }
    private static final class Tarifa implements Serializable { private static final long serialVersionUID=1L; int id, pabellonId; String posicion, concepto; BigDecimal importe; LocalDate desde; }
    private static final class Sepelio implements Serializable { private static final long serialVersionUID=1L; int id, difuntoId, nichoId, responsableId; LocalDateTime fechaHora; LocalDate inicio, vencimiento; String sepulturero, estado; BigDecimal costo; }
    private static final class Movimiento implements Serializable { private static final long serialVersionUID=1L; int id, sepelioId; String tipo, origen, destino, motivo; LocalDate fecha; }
    private static final class Notificacion implements Serializable { private static final long serialVersionUID=1L; int id, sepelioId; String numero, estado, motivo; LocalDate fecha; }
    private static final class Pago implements Serializable { private static final long serialVersionUID=1L; int id, sepelioId, responsableId; String concepto, medio, referencia, comprobante; BigDecimal importe; LocalDate fecha; }
}
