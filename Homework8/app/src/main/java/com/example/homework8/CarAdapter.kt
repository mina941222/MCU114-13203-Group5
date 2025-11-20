package com.example.homework8

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class CarAdapter(
    private var cars: List<UsedCar>,
    // 🔑 變動 1: 移除 onDeleteClick 參數，因為刪除由 MainActivity 的主按鈕處理
    private val onItemClick: (UsedCar) -> Unit // 點擊項目 (用於載入修改/刪除的資料)
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val carId: TextView = view.findViewById(R.id.tv_car_id)
        val carBrand: TextView = view.findViewById(R.id.tv_car_brand)
        val carDetails: TextView = view.findViewById(R.id.tv_car_details)
        // 🔑 變動 2: 移除對 btn_delete 的引用
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]

        holder.carId.text = "#${car.id}"
        holder.carBrand.text = car.brand
        holder.carDetails.text = String.format(Locale.getDefault(),
            "%d 年 | $%,d", car.year, car.price)

        // 點擊整個項目，用於載入資料到上方的輸入框
        holder.itemView.setOnClickListener {
            onItemClick(car)
        }

        // 🔑 變動 3: 移除刪除按鈕的點擊事件
    }

    override fun getItemCount(): Int = cars.size

    // 外部呼叫此函數來更新列表資料 (讀取功能的核心)
    fun updateList(newCars: List<UsedCar>) {
        cars = newCars
        notifyDataSetChanged()
    }
}