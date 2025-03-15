package my.receipt.tracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnCreateExpense = findViewById<Button>(R.id.btnCreateExpense)
        val btnViewExpenses = findViewById<Button>(R.id.btnViewExpenses)

        btnCreateExpense.setOnClickListener {
            val intent = Intent(this, CreateExpenseActivity::class.java)
            startActivity(intent)
        }

        btnViewExpenses.setOnClickListener {
            val intent = Intent(this, ViewExpensesActivity::class.java)
            startActivity(intent)
        }
    }
}
