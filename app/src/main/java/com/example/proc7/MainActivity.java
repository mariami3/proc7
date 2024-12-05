package com.example.proc7;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {

    private EditText titleText, producerText;
    private Uri image;
    private MaterialButton imageButton;
    private ImageView imageView;
    private Button addBtn, delBtn, updateBtn;
    private RecyclerView recyclerView;
    private ClothAdapter adapter;
    private String selectedBook;
    private List<Cloth> clothList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Paper.init(this);

        titleText = findViewById(R.id.titleText);
        producerText = findViewById(R.id.producerText);
        addBtn = findViewById(R.id.addButton);
        delBtn = findViewById(R.id.deleteButton);
        updateBtn = findViewById(R.id.updateButton);
        imageButton = findViewById(R.id.imageButton);
        imageView = findViewById(R.id.imageView);
        recyclerView = findViewById(R.id.recyclerView);

        clothList = loadClothList();
        adapter = new ClothAdapter(clothList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        ActivityResultLauncher<Intent> galleryResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            image = result.getData().getData();
                            imageView.setImageURI(image);
                        }
                    }
                }
        );

        imageButton.setOnClickListener(v -> {
            image = null;
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            galleryResultLauncher.launch(galleryIntent);
        });

        addBtn.setOnClickListener(view -> {
            String title = titleText.getText().toString().trim();
            String producer = producerText.getText().toString().trim();
            if (!title.isEmpty() && !producer.isEmpty() && image != null) {
                Cloth cloth = new Cloth(title, producer, image);
                Paper.book().write(title, cloth);
                updateClothList();
                clearInputs();
            } else {
                Toast.makeText(this, "Поля не должны быть пустыми", Toast.LENGTH_SHORT).show();
            }
        });

        delBtn.setOnClickListener(view -> {
            if (selectedBook != null) {
                Paper.book().delete(selectedBook);
                updateClothList();
                clearInputs();
                selectedBook = null;
            } else {
                Toast.makeText(this, "Выберите книгу для удаления", Toast.LENGTH_SHORT).show();
            }
        });

        updateBtn.setOnClickListener(view -> {
            if (selectedBook == null) {
                Toast.makeText(this, "Выберите книгу для обновления", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = titleText.getText().toString().trim();
            String author = producerText.getText().toString().trim();

            if (!title.isEmpty() && !author.isEmpty() && image != null) {
                if (!selectedBook.equals(title)) {
                    Paper.book().delete(selectedBook);
                }

                Cloth updatedCloth = new Cloth(title, author, image);
                Paper.book().write(title, updatedCloth);

                updateClothList();
                clearInputs();
            } else {
                Toast.makeText(this, "Поля не должны быть пустыми", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Cloth> loadClothList() {
        List<Cloth> list = new ArrayList<>();
        for (String key : Paper.book().getAllKeys()) {
            Cloth cloth = Paper.book().read(key);
            if (cloth != null) {
                list.add(cloth);
            }
        }
        return list;
    }


    private void updateClothList() {
        clothList = loadClothList();
        adapter.setClothList(clothList);
        adapter.notifyDataSetChanged();
    }

    private void clearInputs() {
        titleText.setText("");
        producerText.setText("");
        imageView.setImageDrawable(null);
        selectedBook = null;
    }

    private class ClothAdapter extends RecyclerView.Adapter<ClothViewHolder> {
        private List<Cloth> clothList;

        public ClothAdapter(List<Cloth> clothList) {
            this.clothList = clothList;
        }

        public void setClothList(List<Cloth> clothList) {
            this.clothList = clothList;
        }


        @Override
        public ClothViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cloth_item, parent, false);
            return new ClothViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ClothViewHolder holder, int position) {
            Cloth cloth = clothList.get(position);
            holder.bind(cloth);
            holder.itemView.setOnClickListener(v -> {
                selectedBook = cloth.getTitle();
                titleText.setText(cloth.getTitle());
                producerText.setText(cloth.getProducer());
                imageView.setImageURI(cloth.getImageUri());
            });
        }

        @Override
        public int getItemCount() {
            return clothList.size();
        }
    }

    private class ClothViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView producerTextView;
        private final ImageView imageView;

        public ClothViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            producerTextView = itemView.findViewById(R.id.producerTextView);
            imageView = itemView.findViewById(R.id.imageViewCloth);
        }

        public void bind(Cloth cloth) {
            titleTextView.setText(cloth.getTitle());
            producerTextView.setText(cloth.getProducer());
            if (cloth.getImageUri() != null) {
                try {
                    Log.d("ImageLoading", "Loading image from: " + cloth.getImageUri()); // Добавлено для отладки
                    InputStream inputStream = itemView.getContext().getContentResolver().openInputStream(cloth.getImageUri());
                    if (inputStream != null) { // Проверка на null InputStream
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) { // Проверка на null Bitmap
                            imageView.setImageBitmap(bitmap);
                        } else {
                            Log.e("ImageLoading", "BitmapFactory.decodeStream returned null");
                        }
                        inputStream.close();
                    } else {
                        Log.e("ImageLoading", "InputStream is null");
                    }
                } catch (IOException e) {
                    Log.e("ImageLoading", "Error loading image: ", e);
                }
            }
        }


    }
}
