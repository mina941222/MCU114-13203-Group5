package com.example.homework2;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView txtShow;
    private Button btnZero, btnOne, btnTwo, btnThree, btnFour, btnFive,
            btnSix, btnSeven, btnEight, btnNine, btnStar, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 取得資源類別檔中的介面元件
        txtShow = findViewById(R.id.txtShow);
        btnZero = findViewById(R.id.btnZero);
        btnOne = findViewById(R.id.btnOne);
        btnTwo = findViewById(R.id.btnTwo);
        btnThree = findViewById(R.id.btnThree);
        btnFour = findViewById(R.id.btnFour);
        btnFive = findViewById(R.id.btnFive);
        btnSix = findViewById(R.id.btnSix);
        btnSeven = findViewById(R.id.btnSeven);
        btnEight = findViewById(R.id.btnEight);
        btnNine = findViewById(R.id.btnNine);
        btnStar = findViewById(R.id.btnStar);
        btnClear = findViewById(R.id.btnClear);

        // 設定 button 元件 Click 事件共用 myListener
        btnZero.setOnClickListener(myListener);
        btnOne.setOnClickListener(myListener);
        btnTwo.setOnClickListener(myListener);
        btnThree.setOnClickListener(myListener);
        btnFour.setOnClickListener(myListener);
        btnFive.setOnClickListener(myListener);
        btnSix.setOnClickListener(myListener);
        btnSeven.setOnClickListener(myListener);
        btnEight.setOnClickListener(myListener);
        btnNine.setOnClickListener(myListener);
        btnStar.setOnClickListener(myListener);
        btnClear.setOnClickListener(myListener);
    }

    // 定義 onClick() 方法
    private final View.OnClickListener myListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s = txtShow.getText().toString();

            if (v.getId() == R.id.btnZero) {
                txtShow.setText(s + "0");
            } else if (v.getId() == R.id.btnOne) {
                txtShow.setText(s + "1");
            } else if (v.getId() == R.id.btnTwo) {
                txtShow.setText(s + "2");
            } else if (v.getId() == R.id.btnThree) {
                txtShow.setText(s + "3");
            } else if (v.getId() == R.id.btnFour) {
                txtShow.setText(s + "4");
            } else if (v.getId() == R.id.btnFive) {
                txtShow.setText(s + "5");
            } else if (v.getId() == R.id.btnSix) {
                txtShow.setText(s + "6");
            } else if (v.getId() == R.id.btnSeven) {
                txtShow.setText(s + "7");
            } else if (v.getId() == R.id.btnEight) {
                txtShow.setText(s + "8");
            } else if (v.getId() == R.id.btnNine) {
                txtShow.setText(s + "9");
            } else if (v.getId() == R.id.btnStar) {
                txtShow.setText(s + "*");
            } else if (v.getId() == R.id.btnClear) {
                txtShow.setText("電話號碼：");
            }
        }
    };
}





