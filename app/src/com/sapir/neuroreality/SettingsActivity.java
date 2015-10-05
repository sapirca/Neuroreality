package com.sapir.neuroreality;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.sapir.neuroreality.R;


public class SettingsActivity extends Activity implements OnClickListener {
    Button button;
    RadioGroup genderRadioGroup;
    EditText name;
    EditText age;
    EditText box;
    private DataMediator dataMediator = DataMediator.getInstance();


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Get the ids of view objects
        findAllViewsId();

        button.setOnClickListener(this);
    }

    private void findAllViewsId() {
        button = (Button) findViewById(R.id.submit);
        name = (EditText) findViewById(R.id.name);
        age = (EditText) findViewById(R.id.age);
        box = (EditText) findViewById(R.id.box);

        genderRadioGroup = (RadioGroup) findViewById(R.id.gender);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), VideoListActivity.class);

        dataMediator.setUserName(name.getText().toString());
        dataMediator.setUserAge(age.getText().toString());
        int id = genderRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) findViewById(id);
        dataMediator.setUserGender(radioButton.getText().toString());
        dataMediator.setBoxModel(box.getText().toString());

        //start the DisplayActivity
        startActivity(intent);
    }

}
