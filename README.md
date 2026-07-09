# 🌿 JamuSync – Integrated Business Management System for Jamu MSE

An integrated desktop business management system for a traditional Indonesian herbal beverage (**Jamu**) enterprise. JamuSync digitalizes business operations such as inventory management, sales, staff administration, financial reporting, voucher management, and customer interaction.

Built with **Java 21**, **JavaFX**, **SQLite**, and **Maven**.

---

## 👥 Team QuantaLabs

| Name | Student ID |
|------|------------|
| Hassan Waleed Hassan Abdo Al-Awadhi | 25523274 |
| Rama Walta Alinta Elsaprike | 25523085 |
| Nada Taufik Shahbal | 25523266 |
| Laraib Arshad | 25523275 |

- **Course:** Fundamentals of Application Development & Software Engineering
- **Institution:** Universitas Islam Indonesia
- **Client:** Mr. Yudi's Jamu Herbal Beverage Enterprise

---

## 📖 Project Overview

JamuSync is an integrated management system designed for Micro and Small Enterprises (MSEs) that produce traditional Indonesian herbal beverages (Jamu). It replaces manual business processes with a digital JavaFX desktop application, letting owners and staff manage products, inventory, transactions, reports, vouchers, and customer services.

**Architecture:** Model–View–Controller (MVC) combined with the DAO (Data Access Object) pattern.

```
User → JavaFX Interface (FXML) → Controllers → Services → DAO Layer → SQLite Database
```

---

## ✨ Features

- 🔐 **Authentication** — role-based Owner / Staff login
- 📦 **Product Management** — full CRUD, stock tracking, images
- 👥 **Staff Management** — create / update / deactivate staff accounts
- 📊 **Inventory** — real-time tracking, manual adjustment, low-stock alerts, history
- 💳 **Sales POS** — cart, voucher application, automatic stock deduction
- 🎟 **Voucher Management** — fixed / percentage discounts with expiry & usage limits
- 📈 **Financial Reports** — Pie, Bar, and Line chart visualizations
- 🌿 **Guest Features** — catalog, Herb Matrix, Jamu Mixer, Symptom Sync, cart
- 🌐 **Multi-language** — English, Indonesian (Bahasa Indonesia), Javanese (Basa Jawa)
- 🤖 **AI Chatbot** — herbal recommendations via the Google Gemini API

---

## 🧠 OOP Concepts Demonstrated (with real code)

Every snippet below is copied from the actual project source. The file path is shown above each block.

### 1. Encapsulation

Model fields are `private` and only reachable through public getters/setters, so each object controls its own data.

**`jamusync/src/main/java/com/quantalabs/jamusync/model/Product.java`**

```java
public class Product extends BaseModel {
    private String name;
    private String description;
    private double price;
    private int stock;
    private boolean isActive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
```

### 2. Inheritance

A three-level chain — `User` **is a** `Person`, and a `Person` **is a** `BaseModel` — so shared fields are written once in the parent and inherited by the children.

**`jamusync/src/main/java/com/quantalabs/jamusync/model/BaseModel.java`** — the parent of every model:

```java
public abstract class BaseModel {

    // Shared by every model; inherited (not re-declared) by the children.
    protected int id;
    protected String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Every child must describe itself in its own way.
    public abstract String getSummary();
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/model/Person.java`** — extends `BaseModel`, adds `fullName`:

```java
public abstract class Person extends BaseModel {

    protected String fullName;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/model/User.java`** — extends `Person`, so it inherits `fullName` from `Person` **and** `id` / `createdAt` from `BaseModel`:

```java
// A User IS-A Person, and a Person IS-A BaseModel. So by extending Person,
// User inherits fullName from Person AND id/createdAt from BaseModel.
public class User extends Person {
    private String username;
    private String passwordHash;
    private String role; // 'owner' or 'staff'
    private boolean isActive;
```

### 3. Abstraction

Abstraction shows up in two places: an **abstract method** that hides *what* without saying *how*, and the **DAO layer** that hides all SQL from the rest of the app.

**`jamusync/src/main/java/com/quantalabs/jamusync/model/BaseModel.java`** — an abstract method (no body):

```java
    /**
     * An abstract method has NO body here - just a signature ending in ";".
     * By declaring it abstract we force every child class to write its own
     * version of getSummary(). This lets us ask any model "describe yourself".
     */
    public abstract String getSummary();
```

**`jamusync/src/main/java/com/quantalabs/jamusync/dao/ProductDAO.java`** — the DAO hides the SQL; callers just get a `List<Product>` and never see a query:

```java
public List<Product> getAllActiveProducts() {
    List<Product> products = new ArrayList<>();
    String sql = "SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC";
    try (Connection conn = DatabaseManager.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {

        while (rs.next()) {
            products.add(mapResultSetToProduct(rs));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return products;
}
```

### 4. Polymorphism

The parent declares `getSummary()`; each child **overrides** it with different behaviour. Calling `getSummary()` on any model runs the correct version for that type.

**`jamusync/src/main/java/com/quantalabs/jamusync/model/Product.java`**

```java
@Override
public String getSummary() {
    return name + " - " + price + " (stock: " + stock + ")";
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/model/User.java`**

```java
@Override
public String getSummary() {
    return username + " (" + role + ")";
}
```

### 5. Controller Inheritance (`BaseController`)

Controllers share helper methods through a common parent, removing duplicate code.

**`jamusync/src/main/java/com/quantalabs/jamusync/controller/BaseController.java`**

```java
public class BaseController {

    protected void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("JamuSync - Info");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected User getCurrentUser() {
        return JamuSyncApp.getCurrentUser();
    }
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/controller/LoginController.java`** — extends it and reuses `navigateTo()`:

```java
// Extends BaseController to reuse its shared helper methods (navigateTo, etc.).
public class LoginController extends BaseController {
    ...
    if ("owner".equalsIgnoreCase(user.getRole())) {
        navigateTo("/com/quantalabs/jamusync/fxml/OwnerDashboard.fxml", event);
    } else if ("staff".equalsIgnoreCase(user.getRole())) {
        navigateTo("/com/quantalabs/jamusync/fxml/StaffDashboard.fxml", event);
    }
```

---

## 📚 Data Structures (DSA) (with real code)

### 1. ArrayList / ObservableList

`ObservableList` backs the JavaFX tables and cart so the UI updates automatically; DAOs return plain `ArrayList`s.

**`jamusync/src/main/java/com/quantalabs/jamusync/util/CartManager.java`**

```java
// The list of items in the cart. It is shared by every screen.
private static final ObservableList<CartItem> items = FXCollections.observableArrayList();

// Add an item. If the same product is already in the cart, just add more.
public static void addItem(String name, double price, int qty) {
    for (CartItem item : items) {
        if (item.getProductName().equals(name)) {
            item.setQuantity(item.getQuantity() + qty);
            return;
        }
    }
    items.add(new CartItem(name, price, qty));
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/dao/ProductDAO.java`** — an `ArrayList` collects the query results:

```java
List<Product> products = new ArrayList<>();
...
while (rs.next()) {
    products.add(mapResultSetToProduct(rs));
}
```

### 2. Queue (FIFO)

Pending guest orders are handled oldest-first using a `Queue` (`LinkedList`), with classic enqueue / dequeue / peek operations.

**`jamusync/src/main/java/com/quantalabs/jamusync/util/PendingOrderQueue.java`**

```java
// The Queue that holds all the pending orders.
private final Queue<Transaction> orders = new LinkedList<>();

public void addOrder(Transaction order) {
    orders.add(order); // enqueue - add to the back of the queue
}

public Transaction processNextOrder() {
    return orders.poll(); // dequeue - remove from the front of the queue
}

public Transaction peekNextOrder() {
    return orders.peek(); // peek - view the front of the queue
}
```

### 3. Map-based Aggregation (chart data)

Sales are aggregated per product using SQL `GROUP BY` and returned as `Map.Entry` (product name → total quantity) pairs, which the charts then plot.

**`jamusync/src/main/java/com/quantalabs/jamusync/dao/TransactionItemDAO.java`** — build the aggregated pairs:

```java
public List<Map.Entry<String, Integer>> getSalesByProduct(String startDate, String endDate) {
    List<Map.Entry<String, Integer>> results = new ArrayList<>();
    String sql = "SELECT p.name, SUM(ti.quantity) AS total_qty " +
                 "FROM transaction_items ti " +
                 "JOIN transactions t ON ti.transaction_id = t.id " +
                 "JOIN products p ON ti.product_id = p.id " +
                 "WHERE t.status = 'Completed' " +
                 "GROUP BY p.name ORDER BY total_qty DESC LIMIT 5";
    ...
    while (rs.next()) {
        String name = rs.getString("name");
        int qty = rs.getInt("total_qty");
        results.add(new AbstractMap.SimpleEntry<>(name, qty));
    }
    return results;
}
```

**`jamusync/src/main/java/com/quantalabs/jamusync/controller/OwnerDashboardController.java`** — feed those pairs into the pie chart:

```java
List<Map.Entry<String, Integer>> sales = transactionItemDAO.getSalesByProduct(today, today);
dashPieChart.getData().clear();
for (Map.Entry<String, Integer> entry : sales) {
    dashPieChart.getData().add(
        new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
}
```

---

## 📂 Project Structure

```
JamuSync
└── jamusync
    ├── src
    │   └── main
    │       ├── java
    │       │   └── com/quantalabs/jamusync
    │       │       ├── controller   (BaseController + screen controllers)
    │       │       ├── dao          (SQL data access objects)
    │       │       ├── database     (DatabaseManager, SQLite setup)
    │       │       ├── model        (BaseModel, Person, User, Product, ...)
    │       │       ├── service      (TransactionService, Gemini client, ...)
    │       │       └── util         (CartManager, PendingOrderQueue, LanguageManager)
    │       └── resources
    │           ├── fxml
    │           ├── css
    │           ├── i18n             (messages_en / _id / _jw .properties)
    │           └── images
    └── pom.xml
```

---

## 🚀 How to Run

**Requirements:** Java 21 or newer, and Maven.

Clone the repository:

```bash
git clone https://github.com/UFES-CLASS/final-project-quantalabs.git
```

Run the application (the Maven project lives in the `jamusync/` sub-folder):

```bash
cd final-project-quantalabs/jamusync
mvn javafx:run
```

### 🔑 Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Owner | `admin` | `admin123` |
| Staff | `staff1` | `staff123` |

---

## 💻 Technologies Used

Java 21 · JavaFX · FXML · SQLite · JDBC · Maven · CSS · MVC Architecture · DAO Pattern · Google Gemini API · Git & GitHub

---

## 📜 License & Acknowledgements

Developed for academic purposes as part of the **Fundamentals of Application Development & Software Engineering** course at **Universitas Islam Indonesia**.

Special thanks to Universitas Islam Indonesia, our lecturers and teaching assistants, Mr. Yudi's Jamu Herbal Beverage Enterprise, and Team QuantaLabs.
