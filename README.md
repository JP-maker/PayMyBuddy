# Pay My Buddy
## Technical:

1. Spring Boot 3.4.5
   - Spring Web
   - Spring Data JPA
   - Spring Security
   - Spring Boot DevTools
   - Jacoco
   - Surefire
   - Javadocs
   - Lombok
   - MySQL
2. Java 21
3. Thymeleaf
4. PicoCSS

## Database:
SQL Script for creating the database and tables:
   - `resources/data.sql`

Picture of the database schema:
   - `resources/db-schema.png`

## Run the Application:
1. Create a MySQL database with script
2. Edit the `application.properties` file to set the database connection:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/paymybuddy
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```
3. Run the application using your IDE or command line:
   ```bash
    ./mvnw spring-boot:run
    ```
   
4. Access the application at `http://localhost:8888`
5. You can use the following credentials to log in:
   - **Email:alice@mail.fr**
   - **Password:Md12345**



