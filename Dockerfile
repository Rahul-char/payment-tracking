From openjdk:17-oracle
Copy ./target/payments-0.0.1-SNAPSHOT.jar payments-service.jar
CMD ["java", "-jar", "payments-service.jar"]