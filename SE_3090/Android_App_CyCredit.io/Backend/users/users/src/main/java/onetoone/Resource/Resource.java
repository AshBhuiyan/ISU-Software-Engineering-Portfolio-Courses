package onetoone.Resource;
import jakarta.persistence.*;
import onetoone.Users.User;


    @Entity
    @Table(name = "resource")
    public class Resource {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private int id;

        private int turnsLeft;
        private double money;
        private double credit;
        private double creditLimit = 1500.0; // Default credit limit
        private int currentMonth = 1; // Default current month

        @OneToOne
        @JoinColumn(name = "user_id", referencedColumnName = "id")
        private User user;

        public Resource() {
        }

        public Resource(int turnsLeft, double money, double credit) {
            this.turnsLeft = turnsLeft;
            this.money = money;
            this.credit = credit;
            this.creditLimit = 1500.0; // Default credit limit
            this.currentMonth = 1; // Default current month
        }

        public Resource(int turnsLeft, double money, double credit, double creditLimit, int currentMonth) {
            this.turnsLeft = turnsLeft;
            this.money = money;
            this.credit = credit;
            this.creditLimit = creditLimit;
            this.currentMonth = currentMonth;
        }

        // --- Getters and Setters ---
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTurnsLeft() {
            return turnsLeft;
        }

        public void setTurnsLeft(int turnsLeft) {
            this.turnsLeft = turnsLeft;
        }

        public double getMoney() {
            return money;
        }

        public void setMoney(double money) {
            this.money = money;
        }

        public double getCredit() {
            return credit;
        }

        public void setCredit(double credit) {
            this.credit = credit;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public double getCreditLimit() {
            return creditLimit;
        }

        public void setCreditLimit(double creditLimit) {
            this.creditLimit = creditLimit;
        }

        public int getCurrentMonth() {
            return currentMonth;
        }

        public void setCurrentMonth(int currentMonth) {
            this.currentMonth = currentMonth;
        }
    }

