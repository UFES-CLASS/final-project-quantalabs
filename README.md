# 🌿 JamuSync – Integrated Business Management System for Jamu MSE

An integrated desktop business management system for a traditional Indonesian herbal beverage (**Jamu**) enterprise. JamuSync was developed using **Java 17**, **JavaFX**, **SQLite**, and **Maven** to digitalize business operations such as inventory management, sales, staff administration, financial reporting, voucher management, and customer interaction.

---

# 👥 Team QuantaLabs

| Name | Student ID |

|------|------------|

| Hassan Waleed Hassan Abdo Al-Awadhi | 25523274 |

| Laraib Arshad | 25523275 |

| Nada Taufik Shahbal | 25523266 |

| Rama Walta Alinta Elsaprike | 25523085 |

Course: Fundamentals of Application Development & Software Engineering

Institution:** Universitas Islam Indonesia

Client: Mr. Yudi's Jamu Herbal Beverage Enterprise

---

# 📖 Project Overview

JamuSync is an integrated management system designed for Micro and Small Enterprises (MSEs) that produce traditional Indonesian herbal beverages (Jamu). The application replaces manual business processes with a digital solution, enabling business owners and staff to efficiently manage products, inventory, transactions, reports, vouchers, and customer services through an intuitive JavaFX desktop application.

---

# ✨ Features

## 🔐 Authentication

- Secure Owner login

- Secure Staff login

- Role-based access control

## 📦 Product Management

- Add products

- Edit products

- Delete products

- Search products

- Manage categories

- Automatic stock updates

## 👥 Staff Management

- Create staff accounts

- Update staff information

- Delete staff accounts

- Username validation

## 📊 Inventory Management

- Real-time inventory tracking

- Manual stock adjustment

- Low stock alerts

- Inventory history

## 💳 Sales Point of Sale (POS)

- Process customer transactions

- Shopping cart

- Voucher application

- Automatic stock deduction

- Receipt generation

## 🎟 Voucher Management

- Create vouchers

- Edit vouchers

- Delete vouchers

- Expiration date validation

## 📈 Financial Reports

- Sales reports

- Revenue reports

- Profit & Loss calculations

- Pie Chart visualization

- Bar Chart visualization

- Line Chart visualization

## 🌿 Guest Features

- Browse Product Catalog

- Herb Matrix

- Jamu Mixer

- Symptom Sync

- Shopping Cart

## 🤖 AI Chatbot

- Herbal product recommendations

- Symptom-based suggestions

- Customer assistance

- Product information

---

# 🏗 Software Architecture

JamuSync follows the **Model–View–Controller (MVC)** architecture combined with the **DAO (Data Access Object)** design pattern.

```

User

   │

   ▼

JavaFX Interface (FXML)

   │

   ▼

Controllers

   │

   ▼

Services

   │

   ▼

DAO Layer

   │

   ▼

SQLite Database

```

---

# 💻 Technologies Used

- Java 17

- JavaFX

- FXML

- SQLite

- JDBC

- Maven

- CSS

- MVC Architecture

- DAO Pattern

- Git & GitHub

---

# 📂 Project Structure

```

JamuSync

│

├── src

│   └── main

│       ├── java

│       │   └── com.quantalabs.jamusync

│       │       ├── controller

│       │       ├── dao

│       │       ├── database

│       │       ├── model

│       │       ├── service

│       │       └── util

│       │

│       └── resources

│           ├── fxml

│           ├── css

│           └── images

│

├── pom.xml

└── README.md

```

---

# 📚 Course Concepts Implemented

This project demonstrates the knowledge acquired throughout the course:

- Object-Oriented Programming (OOP)

- JavaFX GUI Development

- FXML Interface Design

- MVC Software Architecture

- DAO Design Pattern

- SQLite Database Integration

- JDBC Connectivity

- CRUD Operations

- Java Collections (ArrayList & ObservableList)

- Exception Handling

- Data Visualization (PieChart, BarChart, LineChart)

- Git Version Control

- Software Engineering Best Practices

---

# 🚀 How to Run

## Requirements

- Java 17 or newer

- Maven

## Clone Repository

```bash

git clone https://github.com/UFES-CLASS/final-project-quantalabs.git

```

## Run Application

```bash

cd jamusync

mvn javafx:run

```

---

# 🔑 Default Login Credentials

## 👑 Owner

| Username | Password |

|----------|----------|

| admin | admin123 |

## 👨‍💼 Staff

| Username | Password |

|----------|----------|

| staff1 | staff123 |


---

# 📄 Documentation

The project includes:

- Software Design Document (SDD)

- Developer Handbook

- Testing Matrix

- Presentation Slides

---

# 🔮 Future Improvements

- Cloud database synchronization

- Mobile application

- Barcode scanner support

- Customer loyalty system

- Online ordering

- Advanced business analytics

---

# 📜 License

This project was developed for academic purposes as part of the **Fundamentals of Application Development & Software Engineering** course at **Universitas Islam Indonesia**.

---

# ❤️ Acknowledgements

Special thanks to:

- Universitas Islam Indonesia

- Our lecturers and teaching assistants

- Mr. Yudi's Jamu Herbal Beverage Enterprise

- Team QuantaLabs
