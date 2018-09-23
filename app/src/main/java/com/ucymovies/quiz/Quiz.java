package com.ucymovies.quiz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class Quiz extends Activity{
	
	//submit button
	Button nextButton;

	
	//choice
	private RadioGroup radioGroup;
	private RadioButton radio0;
	private RadioButton radio1;
	private RadioButton radio2;
	private RadioButton radio3;
	private TextView questionText;
	private TextView feedbackText;
	private TextView timeLabel;



	private static long duration = 60 * 1000 * 2; // total time for the quiz
	private static long passedTime = 0;
	private static Handler timeHandler = new Handler();
	private static long timeStart = SystemClock.uptimeMillis();
	private static boolean initialStartTime = false;
	
	//update timer
	private Runnable updateTask = new Runnable() {	
		public void run() {


			long now = SystemClock.uptimeMillis(); //counted in milliseconds since the system was booted
			long remaining = duration - passedTime - (now - timeStart);
			passedTime = now - timeStart + passedTime;
			timeStart = now;

			if (Statistics.returnflag()==1)
			{

				passedTime=0;
			}
			Statistics.changeflag(0);


			if (remaining > 0) {
				int seconds = (int) (remaining / 1000);
				int minutes = seconds / 60;
				seconds     = seconds % 60;

				if (seconds < 10) {
					timeLabel.setText("" + minutes + ":0" + seconds);
				} else {
					timeLabel.setText("" + minutes + ":" + seconds);            
				}

				timeHandler.postDelayed(this, 1000);
			}
			else
			{
				timeHandler.removeCallbacks(this);
				initialStartTime = false;
				passedTime = 0;
				timeStart = SystemClock.uptimeMillis();
				

				Statistics.writeToDatabase(getApplication());

				//go to statistics page
				String result = "Right: " + Statistics.getCurrentCorrectAnswers() + "\n Wrong: "+ Statistics.getCurrentWrongAnswers();
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
				Statistics.setCurrentCorrectAnswers(0);
				Statistics.setCurrentWrongAnswers(0);

				Intent intent = new Intent(Quiz.this, MainActivity.class);
				startActivity(intent);
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_quiz);



        questionText = (TextView)findViewById(R.id.question_textView);
        feedbackText = (TextView)findViewById(R.id.feedback);
        
        timeLabel = (TextView) findViewById(R.id.timeLabel);
        timeLabel.setTextColor(Color.RED);


        if(!initialStartTime){
        	timeStart = SystemClock.uptimeMillis();
        	initialStartTime = true;
        }
        timeHandler.post(updateTask);
        
        radio0 = (RadioButton)findViewById(R.id.radioButton0);
        radio1 = (RadioButton)findViewById(R.id.radioButton1);
        radio2 = (RadioButton)findViewById(R.id.radioButton2);
        radio3 = (RadioButton)findViewById(R.id.radioButton3);

		radio0.setEnabled(true);
		radio1.setEnabled(true);
		radio2.setEnabled(true);
		radio3.setEnabled(true);
        
        final Question question = QuestionUtils.getRandomQuestion(this); // get random question
        questionText.setText(question.getQuestion());	// show
        String[] choices = question.getAnswers();		// get answers - choices
        radio0.setText(choices[0]);
        radio1.setText(choices[1]);
        radio2.setText(choices[2]);
        radio3.setText(choices[3]);


        
        radioGroup = (RadioGroup)findViewById(R.id.selectionGroup);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener(){
        	@Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
        		int selected = -1;
        		if(checkedId == R.id.radioButton0){
        			selected = 0;
        		}
        		else if(checkedId == R.id.radioButton1){
        			selected = 1;
        		}
        		else if(checkedId == R.id.radioButton2){
        			selected = 2;
        		}
        		else if(checkedId == R.id.radioButton3){
        			selected = 3;
        		}
        		if(question.getCorrectAnswer() == selected){
        			feedbackText.setText("Correct!");
                    feedbackText.setTextSize(30);
        			feedbackText.setTextColor(Color.GREEN);
        			Statistics.incrementCurrentCorrectAnswers();
					radio0.setEnabled(false);
					radio1.setEnabled(false);
					radio2.setEnabled(false);
					radio3.setEnabled(false);

        		}
        		else{
        			String[] answers = question.getAnswers();
        			int correctChoice = question.getCorrectAnswer();
        			feedbackText.setText("The correct answer should be " + answers[correctChoice]);
        			feedbackText.setTextColor(Color.RED);
                    feedbackText.setTextSize(20);
        			Statistics.incrementCurrentWrongAnswers();
					radio0.setEnabled(false);
					radio1.setEnabled(false);
					radio2.setEnabled(false);
					radio3.setEnabled(false);

        		}

            }
        });
        
        nextButton = (Button)findViewById(R.id.next);
        nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				timeHandler.removeCallbacks(updateTask);
				Statistics.resetIncrement();
				Intent intent = new Intent(Quiz.this, Quiz.class);
				startActivity(intent);
			}
		});


    }


    
    @Override
    protected void onPause (){
    	super.onPause();
    	SharedPreferences prefs= getSharedPreferences(
    			getString(R.string.app_name).toString(), Context.MODE_PRIVATE);
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.clear();
    	editor.putInt("currentRight", Statistics.getCurrentCorrectAnswers());
    	editor.putInt("currentWrong", Statistics.getCurrentWrongAnswers());
    	long time = passedTime + SystemClock.uptimeMillis() - timeStart;
    	editor.putLong("passedTime", time);
    	editor.commit();
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	timeStart = SystemClock.uptimeMillis();
    	SharedPreferences prefs= getSharedPreferences(
    			getString(R.string.app_name).toString(), Context.MODE_PRIVATE);
    	passedTime = prefs.getLong("passedTime", 0L);
    	Statistics.setCurrentCorrectAnswers(
    			prefs.getInt("currentRight", 0));
    	Statistics.setCurrentWrongAnswers(
    			prefs.getInt("currentWrong", 0));
    }
    
//
}
