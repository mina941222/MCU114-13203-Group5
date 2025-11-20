package com.example.homework8

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: CarDatabase
    private lateinit var adapter: CarAdapter
    private var carToUpdate: UsedCar? = null

    private lateinit var etBrand: EditText
    private lateinit var etYear: EditText
    private lateinit var etPrice: EditText
    private lateinit var btnInsert: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDeleteCar: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = CarDatabase.getDatabase(this)

        etBrand = findViewById(R.id.et_brand)
        etYear = findViewById(R.id.et_year)
        etPrice = findViewById(R.id.et_price)
        btnInsert = findViewById(R.id.btn_insert)
        btnUpdate = findViewById(R.id.btn_update)
        btnDeleteCar = findViewById(R.id.btn_delete_car)
        recyclerView = findViewById(R.id.recycler_view_cars)

        setupRecyclerView()

        setupListeners()

        collectCarData()
    }

    private fun setupRecyclerView() {
        adapter = CarAdapter(
            cars = emptyList(),
            onItemClick = { car ->
                selectCarForUpdate(car)
            },
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnInsert.setOnClickListener {
            insertCar()
        }

        btnUpdate.setOnClickListener {
            updateCar()
        }

        btnDeleteCar.setOnClickListener {
            carToUpdate?.let { car ->
                deleteCar(car)
            } ?: Toast.makeText(this, "請先選擇要刪除的項目", Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectCarData() {
        lifecycleScope.launch {
            db.carDao().getAllCars().collect { cars ->
                adapter.updateList(cars)
            }
        }
    }

    private fun insertCar() {
        val brand = etBrand.text.toString()
        val yearStr = etYear.text.toString()
        val priceStr = etPrice.text.toString()

        if (brand.isEmpty() || yearStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show()
            return
        }

        val car = UsedCar(
            brand = brand,
            year = yearStr.toInt(),
            price = priceStr.toInt()
        )

        lifecycleScope.launch {
            db.carDao().insert(car)
            clearInputs()
            Toast.makeText(this@MainActivity, "新增成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectCarForUpdate(car: UsedCar) {
        carToUpdate = car
        etBrand.setText(car.brand)
        etYear.setText(car.year.toString())
        etPrice.setText(car.price.toString())

        btnUpdate.isEnabled = true
        btnDeleteCar.isEnabled = true
        btnInsert.isEnabled = false
        Toast.makeText(this, "已選擇 ID #${car.id}，請修改或點擊刪除", Toast.LENGTH_LONG).show()
    }

    private fun updateCar() {
        val car = carToUpdate ?: run {
            Toast.makeText(this, "請先選擇要修改的項目", Toast.LENGTH_SHORT).show()
            return
        }

        val brand = etBrand.text.toString()
        val yearStr = etYear.text.toString()
        val priceStr = etPrice.text.toString()

        if (brand.isEmpty() || yearStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedCar = car.copy(
            brand = brand,
            year = yearStr.toInt(),
            price = priceStr.toInt()
        )

        lifecycleScope.launch {
            db.carDao().update(updatedCar)
            clearInputs()
            resetUpdateState()
            Toast.makeText(this@MainActivity, "修改成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCar(car: UsedCar) {
        lifecycleScope.launch {
            db.carDao().delete(car)
            if (carToUpdate?.id == car.id) {
                resetUpdateState()
            }
            Toast.makeText(this@MainActivity, "刪除 ID #${car.id} 成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        etBrand.text.clear()
        etYear.text.clear()
        etPrice.text.clear()
    }

    private fun resetUpdateState() {
        carToUpdate = null
        btnUpdate.isEnabled = false
        btnDeleteCar.isEnabled = false
        btnInsert.isEnabled = true
        clearInputs()
    }
}