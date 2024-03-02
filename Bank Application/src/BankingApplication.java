import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class BankingApplication {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/banking_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    private static Map<String, BankAccount> accounts = new HashMap<>();

    public static void main(String[] args) {
        initializeAccountsFromDatabase();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Welcome to the bank. Please select an option:");
             System.out.println("1. Create Account");
            System.out.println("2. Check Balance");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. Exit");

            int choice = getChoice(scanner);

            switch (choice) {
                case 1:
                    createAccount(scanner);
                    break;

                case 2:
                    checkBalance(scanner);
                    break;

                case 3:
                    depositMoney(scanner);
                    break;

                case 4:
                    withdrawMoney(scanner);
                    break;

                case 5:
                    System.out.println("Exiting the application. Goodbye!");
                    System.exit(0);

                default:
                    System.out.println("Invalid choice. Please enter a valid option.");
            }
        }
    }

    private static void initializeAccountsFromDatabase() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM users")) {

            while (resultSet.next()) {
                String accountNumber = resultSet.getString("account_no");
                double balance = resultSet.getDouble("current_balance");
                accounts.put(accountNumber, new BankAccount(accountNumber, balance));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getChoice(Scanner scanner) {
        int choice;
        while (true) {
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
                break;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid integer.");
                scanner.nextLine();
            }
        }
        return choice;
    }

    private static void createAccount(Scanner scanner) {
        System.out.println("Enter your full name:");
        String userName = scanner.nextLine();
        System.out.println("Enter your initial balance:");
        double initialBalance = getDoubleInput(scanner);

        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO users (user_name, account_no, amount) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            String accountNumber = generateAccountNumber();
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, accountNumber);
            preparedStatement.setDouble(3, initialBalance);
            preparedStatement.executeUpdate();

            System.out.println("Account created successfully. Your account number is: " + accountNumber);
            accounts.put(accountNumber, new BankAccount(accountNumber, initialBalance));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String generateAccountNumber() {

        return String.valueOf((int) (Math.random() * 1000000));
    }

    private static void checkBalance(Scanner scanner) {
        System.out.println("Enter your account number:");
        String accountNumber = scanner.nextLine();
        if (accounts.containsKey(accountNumber)) {
            BankAccount account = accounts.get(accountNumber);
            System.out.println("Balance for account " + accountNumber + ": $" + account.getCurrentBalance());
        } else {
            System.out.println("Account not found. Please enter a valid account number.");
        }
    }

    private static void depositMoney(Scanner scanner) {
        System.out.println("Enter your account number:");
        String accountNumber = scanner.nextLine();
        if (accounts.containsKey(accountNumber)) {
            System.out.println("Enter the deposit amount:");
            double depositAmount = getDoubleInput(scanner);
            BankAccount account = accounts.get(accountNumber);
            account.deposit(depositAmount);
            updateDepositInDatabase(accountNumber, account.getTotalDeposits() + depositAmount);
            updateBalanceInDatabase(accountNumber, account.getCurrentBalance() + depositAmount);
            System.out.println("Deposit successful. New balance: $" + account.getCurrentBalance());
        } else {
            System.out.println("Account not found. Please enter a valid account number.");
        }
    }

    private static void withdrawMoney(Scanner scanner) {
        System.out.println("Enter your account number:");
        String accountNumber = scanner.nextLine();
        if (accounts.containsKey(accountNumber)) {
            System.out.println("Enter the withdrawal amount:");
            double withdrawalAmount = getDoubleInput(scanner);
            BankAccount account = accounts.get(accountNumber);
            if (account.getCurrentBalance() >= withdrawalAmount) {
                account.withdraw(withdrawalAmount);
                updateWithdrawalInDatabase(accountNumber, account.getTotalWithdrawals() + withdrawalAmount);
                updateBalanceInDatabase(accountNumber, account.getCurrentBalance() - withdrawalAmount);
                System.out.println("Withdrawal successful. New balance: $" + account.getCurrentBalance());
            } else {
                System.out.println("Insufficient funds. Withdrawal failed.");
            }
        } else {
            System.out.println("Account not found. Please enter a valid account number.");
        }
    }


    private static double getDoubleInput(Scanner scanner) {
        double amount;
        while (true) {
            try {
                amount = scanner.nextDouble();
                scanner.nextLine();
                break;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid number.");
                scanner.nextLine();
            }
        }
        return amount;
    }

    private static void updateBalanceInDatabase(String accountNumber, double newBalance) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE users SET current_balance = ? WHERE account_no = ?")) {
            preparedStatement.setDouble(1, newBalance);
            preparedStatement.setString(2, accountNumber);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void updateDepositInDatabase(String accountNumber, double newDeposit) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE users SET total_deposits = ? WHERE account_no = ?")) {
            preparedStatement.setDouble(1, newDeposit);
            preparedStatement.setString(2, accountNumber);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void updateWithdrawalInDatabase(String accountNumber, double newWithdrawal) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE users SET total_withdrawals = ? WHERE account_no = ?")) {
            preparedStatement.setDouble(1, newWithdrawal);
            preparedStatement.setString(2, accountNumber);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static class BankAccount {
        private String accountNumber;
        private double amount; 
        private double totalDeposits;
        private double totalWithdrawals;
        private double currentBalance;

        public BankAccount(String accountNumber, double initialBalance) {
            this.accountNumber = accountNumber;
            this.amount = initialBalance;
            this.totalDeposits = 0;
            this.totalWithdrawals = 0;
            this.currentBalance = initialBalance;
        }

        public double getAmount() {
            return amount;
        }

        public double getTotalDeposits() {
            return totalDeposits;
        }

        public double getTotalWithdrawals() {
            return totalWithdrawals;
        }

        public double getCurrentBalance() {
            return currentBalance;
        }

        public void deposit(double depositAmount) {
            amount += depositAmount;
        }

        public void withdraw(double withdrawalAmount) {
            amount -= withdrawalAmount;
        }
    }
}

