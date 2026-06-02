

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class Vehicle {
    String id, owner, model, city;
    boolean stolen;

    Vehicle(String id, String owner, String model, String city) {
        this.id = id.toUpperCase();
        this.owner = owner;
        this.model = model;
        this.city = city;
        this.stolen = false;
    }
}

class VehicleSystem {
    HashMap<String, Vehicle> map = new HashMap<>();

    boolean add(Vehicle v) {
        if (map.containsKey(v.id)) return false;
        map.put(v.id, v);
        return true;
    }

    Vehicle search(String id) {
        return map.get(id.toUpperCase());
    }

    Collection<Vehicle> all() {
        return map.values();
    }

    boolean markStolen(String id) {
        Vehicle v = map.get(id.toUpperCase());
        if (v == null || v.stolen) return false;
        v.stolen = true;
        return true;
    }
}

class TrieNode {
    HashMap<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
}

class Trie {
    TrieNode root = new TrieNode();

    void insert(String word) {
        TrieNode cur = root;
        for (char c : word.toUpperCase().toCharArray()) {
            cur.children.putIfAbsent(c, new TrieNode());
            cur = cur.children.get(c);
        }
        cur.isEnd = true;
    }

    TrieNode find(String prefix) {
        TrieNode cur = root;
        for (char c : prefix.toUpperCase().toCharArray()) {
            if (!cur.children.containsKey(c)) return null;
            cur = cur.children.get(c);
        }
        return cur;
    }

    void collect(TrieNode node, String prefix, ArrayList<String> res) {
        if (node.isEnd) res.add(prefix);
        for (char c : node.children.keySet())
            collect(node.children.get(c), prefix + c, res);
    }

    ArrayList<String> getSuggestions(String prefix) {
        ArrayList<String> res = new ArrayList<>();
        TrieNode node = find(prefix);
        if (node != null) collect(node, prefix.toUpperCase(), res);
        return res;
    }
}

class Graph {
    HashMap<String, ArrayList<String>> map = new HashMap<>();

    void addCity(String c) {
        map.putIfAbsent(c.toLowerCase(), new ArrayList<>());
    }

    void addRoad(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        map.putIfAbsent(a, new ArrayList<>());
        map.putIfAbsent(b, new ArrayList<>());

        map.get(a).add(b);
        map.get(b).add(a);
    }

    ArrayList<String> getRoutes(String city) {
        return map.getOrDefault(city.toLowerCase(), new ArrayList<>());
    }
}

class Dashboard extends JFrame {

    VehicleSystem vs = new VehicleSystem();
    Trie trie = new Trie();
    Graph graph = new Graph();

    JTextField searchField = new JTextField();
    JTextArea resultArea = new JTextArea();

    DefaultListModel<String> model = new DefaultListModel<>();
    JList<String> suggestionList = new JList<>(model);

    Color bg = new Color(15, 18, 25);
    Color card = new Color(25, 30, 40);
    Color accent = new Color(0, 200, 255);

    Dashboard() {

        setTitle("Vehicle Stolen Tracker Pro");
        setSize(950, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        JLabel title = new JLabel("Vehicle Stolen Tracker System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(card);
        header.add(title);

        JTabbedPane tabs = new JTabbedPane();

        tabs.add("Register", registerTab());
        tabs.add("All Vehicles", showTab());
        tabs.add("Search", searchTab());
        tabs.add("Graph Routes", graphTab());
        tabs.add("Mark Stolen", stolenTab());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        seed();
        setVisible(true);
    }

    JPanel registerTab() {

        JPanel p = new JPanel(new GridLayout(5,2,10,10));
        p.setBackground(card);

        JTextField id = new JTextField();
        JTextField owner = new JTextField();
        JTextField model = new JTextField();
        JTextField city = new JTextField();
        JButton btn = new JButton("REGISTER");
        styleButton(btn);
        JLabel msg = new JLabel("");
        msg.setForeground(Color.WHITE);
        JLabel idL=new JLabel("ID");
        idL.setForeground(Color.WHITE);
        p.add(idL);
        p.add(id);
        JLabel ownerL=new JLabel("Owner");
        ownerL.setForeground(Color.white);
        p.add(ownerL);
        p.add(owner);
        JLabel modelL=new JLabel("Model");
        modelL.setForeground(Color.white);
        p.add(modelL);
        p.add(model);
        JLabel cityL=new JLabel("City");
        cityL.setForeground(Color.white);
        p.add(cityL);
        p.add(city);
        p.add(btn);
        p.add(msg);

        btn.addActionListener(e -> {

            Vehicle v = new Vehicle(
                    id.getText(),
                    owner.getText(),
                    model.getText(),
                    city.getText()
            );

            if (vs.add(v)) {
                trie.insert(v.id);
                msg.setText("Registered");
            } else {
                msg.setText("Exists");
            }
        });

        return p;
    }

    JPanel showTab() {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(card);

        JTextArea area = new JTextArea();
        styleArea(area);

        JButton btn = new JButton("REFRESH");
        styleButton(btn);

        btn.addActionListener(e -> {
            area.setText("");
            for (Vehicle v : vs.all()) {
                area.append(v.id + " | " + v.owner + " | " +
                        v.model + " | " + v.city + " | " +
                        (v.stolen ? "STOLEN" : "SAFE") + "\n");
            }
        });

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(0, 300));

        p.add(btn, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    JPanel searchTab() {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(card);

        styleArea(resultArea);
        resultArea.setEditable(false);

        suggestionList.setBackground(card);
        suggestionList.setForeground(Color.CYAN);

        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateSuggestions();
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    showResult(searchField.getText());
                }
            }
        });

        suggestionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String id = suggestionList.getSelectedValue();
                if (id != null) {
                    searchField.setText(id);
                    showResult(id);
                }
            }
        });

        p.add(searchField, BorderLayout.NORTH);
        p.add(new JScrollPane(suggestionList), BorderLayout.WEST);
        p.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return p;
    }

    void updateSuggestions() {

        model.clear();

        String text = searchField.getText().trim();
        if (text.isEmpty()) return;

        for (String s : trie.getSuggestions(text)) {
            model.addElement(s);
        }
    }

    void showResult(String id) {

        Vehicle v = vs.search(id);

        if (v == null) {
            resultArea.setText("NOT FOUND");
            return;
        }

        resultArea.setText(
                "ID: " + v.id + "\n" +
                        "Owner: " + v.owner + "\n" +
                        "Model: " + v.model + "\n" +
                        "City: " + v.city + "\n" +
                        "Stolen: " + (v.stolen ? "YES" : "NO")
        );
    }

    JPanel graphTab() {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(card);

        JTextField id = new JTextField();
        JTextArea area = new JTextArea();
        styleArea(area);

        JButton btn = new JButton("SHOW ROUTES");
        styleButton(btn);

        btn.addActionListener(e -> {

            Vehicle v = vs.search(id.getText());
            area.setText("");

            if (v == null) {
                area.setText("NOT FOUND");
                return;
            }

            ArrayList<String> routes = graph.getRoutes(v.city);

            for (String r : routes) {
                area.append(v.city + " -> " + r + "\n");
            }
        });

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(0, 250));

        p.add(id, BorderLayout.NORTH);
        p.add(btn, BorderLayout.CENTER);
        p.add(sp, BorderLayout.SOUTH);

        return p;
    }

    JPanel stolenTab() {

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(card);

        JTextField id = new JTextField();
        JTextArea area = new JTextArea();
        styleArea(area);

        JButton btn = new JButton("MARK STOLEN");
        styleButton(btn);

        btn.addActionListener(e -> {

            if (vs.markStolen(id.getText())) {
                area.setText("MARKED STOLEN");
            } else {
                area.setText("NOT FOUND");
            }
        });

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(0, 250));

        p.add(id, BorderLayout.NORTH);
        p.add(btn, BorderLayout.CENTER);
        p.add(sp, BorderLayout.SOUTH);

        return p;
    }

    void styleButton(JButton b) {
        b.setBackground(accent);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(120, 30));
    }

    void styleArea(JTextArea a) {
        a.setBackground(new Color(20, 22, 30));
        a.setForeground(Color.WHITE);
        a.setFont(new Font("Consolas", Font.PLAIN, 13));
    }

    void seed() {

        vs.add(new Vehicle("LHR-1","Ali","Civic","Lahore"));
        trie.insert("LHR-1");

        vs.add(new Vehicle("KHI-2","Sara","City","Karachi"));
        trie.insert("KHI-2");

        graph.addCity("Lahore");
        graph.addCity("Karachi");
        graph.addCity("Islamabad");

        graph.addRoad("Lahore","Islamabad");
        graph.addRoad("Lahore","Karachi");
        graph.addRoad("Islamabad","Karachi");
    }

    public static void main(String[] args) {
        new Dashboard();
    }
}