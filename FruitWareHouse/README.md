# FruitWareHouse Inventory System

A professional Spring Boot 3.3.2 application for managing fruit inventory.

## 🚀 How to Run the Application

To start the application, run the following command in the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

The application will start on **port 8081**.

### 🛑 How to Stop the Application

To stop the running application, go to the terminal where it is running and press:
- **`Ctrl + C`**

Alternatively, you can close the terminal window or stop the process in your IDE's task manager.

---

## 🧪 Demo & Testing Commands

You can test the API using PowerShell `Invoke-RestMethod` or any REST client (Postman/Curl).

### 1. Create a New Fruit (POST)
Add "Apple" to the inventory.
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/fruits" -Method Post -Body '{"name": "Apple", "price": 1.50, "stockQuantity": 100}' -ContentType "application/json"
```

### 2. List All Fruits (GET)
Retrieve all fruits in the system.
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/fruits" -Method Get
```

### 3. Update Stock Quantity (PUT)
Update the stock for fruit with ID 1 to 150.
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/fruits/1/stock?quantity=150" -Method Put
```

### 4. Find Fruit by Name (GET)
Search for a fruit named "Apple".
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/fruits/name/Apple" -Method Get
```

### 5. Delete a Fruit (DELETE)
Remove fruit with ID 1 from the inventory.
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/fruits/1" -Method Delete
```

---

## 🛠 Project Structure

- **[FruitWarehouseApplication.java](src/main/java/com/manage/fruit/FruitWarehouseApplication.java)**: Entry point.
- **[FruitController.java](src/main/java/com/manage/fruit/controller/FruitController.java)**: REST Endpoints.
- **[FruitService.java](src/main/java/com/manage/fruit/service/FruitService.java)**: Business Logic.
- **[FruitRepository.java](src/main/java/com/manage/fruit/repository/FruitRepository.java)**: JPA Data Access.
- **[Fruit.java](src/main/java/com/manage/fruit/model/Fruit.java)**: Inventory Entity.
- **[legacy/](src/main/java/com/manage/fruit/legacy/)**: Original project files.

## 📊 Database Console
The application uses an H2 in-memory database. You can access the console at:
- **URL**: `http://localhost:8081/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **User**: `sa`
- **Password**: (blank)
