package com.example.homework3

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivitySideDishesBinding

class SideDishesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySideDishesBinding
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySideDishesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CURRENT_ORDER", Order::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CURRENT_ORDER")!!
        }

        binding.btnNext.setOnClickListener {
            val selectedSideDishes = mutableListOf<String>()
            if (binding.cbFries.isChecked) selectedSideDishes.add("Fries")
            if (binding.cbSalad.isChecked) selectedSideDishes.add("Salad")
            if (binding.cbCornCup.isChecked) selectedSideDishes.add("Corn Cup")

            currentOrder.sideDishes = selectedSideDishes

            val intent = Intent(this, DrinkActivity::class.java).apply {
                putExtra("CURRENT_ORDER", currentOrder)
            }
            startActivityForResult(intent, REQUEST_CODE_ORDER_WIZARD)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_WIZARD && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    companion object {
        private const val REQUEST_CODE_ORDER_WIZARD = 1
    }
}