package com.ucymovies.quiz;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class QuestionUtils {

	private static final int QUESTION_NO = 4;
	private static final int OPTION_NO = 4;
	private static SQLiteDatabase db;

	private static class Movie{
		public String id;
		public String title;
		public String year;
		public String director;

		public Movie(String id, String title, String year, String director){
			this.id = id;
			this.title = title;
			this.year = year;
			this.director = director;
		}
	}

	private static class Star{
		public String id;
		public String first_name;
		public String last_name;

		public Star(String id, String first_name, String last_name){
			this.id = id;
			this.first_name = first_name.replace('"', '\0');
			this.last_name =last_name.replace('"', '\0');
		}
		public String getFullName(){
			if(first_name.length() == 0)
				return last_name;
			else
				return "\""+first_name + " " + last_name+"\"";
		}
	}

	public static Question getRandomQuestion(Context context)
	{
		if(db == null){
			db = DbAdapter.getSQLiteDatabase(context);
		}

		Random r = new Random();
		int qNo = r.nextInt(QUESTION_NO);
		Question question = null;
		switch(qNo){
			case 0: question = getQuestionZero();break; // who directed the movie x
			case 1: question = getQuestionOne(); break; // when was the movie x released
			case 2: question = getQuestionTwo();break;	// which star was in the movie x
			case 3: question = getQuestionThree();break;// which star appears in both movies
			default: question = getQuestionZero(); break;
		}
		return question;
	}

	// Who directed the movie X?
	private static Question getQuestionZero(){
		Movie movie = getRandomMovie();
		Set<String> directorSet = getDirectors(movie.director);

		// Build question and options
		String[] directors = new String[OPTION_NO];
		int correctAnswer = 0, i = 0;
		for(String director : directorSet){
			if(director.equals(movie.director)){
				correctAnswer = i;
			}
			directors[i++] = director;
		}

		return new Question(
				"Who directed the movie " + movie.title + "?",
				correctAnswer, directors);
	}

	//When was the movie X released?
	private static Question getQuestionOne(){
		Movie movie = getRandomMovie();
		Set<String> timeSet = getRandomYears(movie.year);

		// Build question and options
		String[] years = new String[OPTION_NO];
		int correctAnswer = 0, i = 0;
		for(String year : timeSet){
			if(year.equals(movie.year)){
				correctAnswer = i;
			}
			years[i++] = year;
		}

		return new Question(
				"When was the movie " + movie.title + " released?",
				correctAnswer, years);
	}

	//Which star was in the movie X?
	private static Question getQuestionTwo(){
		Movie movie = getRandomMovie();
		Set<Star> correctStarSet = getStarsByMovieID(movie.id,true,1);
		Set<Star> starSet = getStarsByMovieID(movie.id,false, 3);

		// Build question and options
		Star star =  (Star) correctStarSet.toArray()[0];
		starSet.add(star);
		String[] options = new String[OPTION_NO];
		int correctAnswer = 0, i = 0;
		for(Star s : starSet){
			if(s.id.equals(star.id)){
				correctAnswer = i;
			}
			options[i++] = s.getFullName();
		}

		return new Question(
				"Which star was in the movie " + movie.title + "?",
				correctAnswer, options);
	}

	//Which star appears in both movies x and y?
	private static Question getQuestionThree(){
		Star star = null;
		Set<Movie> movieSet = null;
		do{
			star = getRandomStar();
			movieSet = getMoviesByStarID(star.id, true, 2, null);
		}while(movieSet.size() != 2);
		Movie[] movies = new Movie[2];
		movies[0] =  (Movie) movieSet.toArray()[0];
		movies[1] =  (Movie) movieSet.toArray()[1];

		Set<Star> starSet = getStarsByMovieID(movies[0].id, false, 3);
		starSet.add(star);

		// Build question and options
		String[] options = new String[OPTION_NO];
		int i = 0;
		int correctAnswer =  0;
		for(Star s :starSet){
			if(s.id.equals(star.id)){
				correctAnswer = i;
			}
			options[i++] = s.getFullName();
		}

		return new Question(
				"Which star appears in both movies "+ movies[0].title + " and "+ movies[1].title+"?",
				correctAnswer, options);
	}



	private static Set<Movie> getMoviesByStarID(String starId, boolean hasStar, int limit, String groupbyClause) {
		StringBuffer qry = new StringBuffer();
		if(hasStar == true){
			qry.append("select * from " + DBInfo.MOVIES +
					" where " + DBInfo._ID + " in (select " + DBInfo.MOVIE_ID + " from " + DBInfo.STARS_IN_MOVIES +
					" where " + DBInfo.STAR_ID + " = " + starId + ") ");
		}
		else{
			qry.append("select * from " + DBInfo.MOVIES +
					" where " + DBInfo._ID + " not in (select " + DBInfo.MOVIE_ID + " from " + DBInfo.STARS_IN_MOVIES +
					" where " + DBInfo.STAR_ID + " = " + starId + ") ");
		}

		if(groupbyClause != null){
			qry.append(" group by "+ groupbyClause);
		}

		if(limit != -1){
			qry.append(" limit " + limit + ";");
		}
		else{
			qry.append(";");
		}

		Cursor cur = db.rawQuery(qry.toString(),null);
		cur.moveToFirst();
		Set<Movie> movieSet = new HashSet<Movie>();
		while(!cur.isAfterLast()){
			movieSet.add(new Movie(cur.getString(0).trim(), cur.getString(1).trim(),
					cur.getString(2).trim(), cur.getString(3).trim()));
			cur.moveToNext();
		}
		cur.close();
		return movieSet;
	}


	private static Set<Star> getStarsByMovieID(String movieId, boolean inMovie, int limit) {
		StringBuffer qry = new StringBuffer();
		if(inMovie == true)
		{
			qry.append("select * from " + DBInfo.STARS +
					" where " + DBInfo._ID + " in (select " + DBInfo.STAR_ID + " from " + DBInfo.STARS_IN_MOVIES +
					" where " + DBInfo.MOVIE_ID + " = " + movieId + ") ");
		}
		else
		{
			qry.append("select * from " + DBInfo.STARS +
					" where " + DBInfo._ID + " not in (select "+ DBInfo.STAR_ID +" from "+ DBInfo.STARS_IN_MOVIES +
					" where "+ DBInfo.MOVIE_ID +" = "+ movieId + ") ");
		}

		if(limit != -1){
			qry.append(" limit " + limit + ";");
		}
		else{
			qry.append(";");
		}

		Cursor cur = db.rawQuery(qry.toString(),null);
		cur.moveToFirst();
		Set<Star> starSet = new HashSet<Star>();
		while(!cur.isAfterLast()){
			starSet.add(new Star(cur.getString(0).trim(), cur.getString(1).trim(), cur.getString(2).trim()));
			cur.moveToNext();
		}
		cur.close();
		return starSet;
	}

	private static Movie getRandomMovie(){
		String whereClause = DBInfo._ID + " >= (abs(random()) % (SELECT MAX(" + DBInfo._ID + ") FROM " + DBInfo.MOVIES + ")) LIMIT 1";
		Cursor cur = db.query(DBInfo.MOVIES,
				new String[] {DBInfo._ID, DBInfo.TITLE, DBInfo.YEAR, DBInfo.DIRECTOR},
				whereClause, null, null, null, null);
		cur.moveToFirst();
		Movie movie = new Movie(cur.getString(0).trim(), cur.getString(1).trim(),
				cur.getString(2).trim(), cur.getString(3).trim());
		cur.close();
		return movie;
	}

	private static Star getRandomStar(){
		String whereClause = DBInfo._ID + " >= (abs(random()) % (SELECT MAX(" + DBInfo._ID + ") FROM " + DBInfo.STARS + ")) LIMIT 1";
		Cursor cur = db.query(DBInfo.STARS,
				new String[] {DBInfo._ID, DBInfo.FIRST_NAME, DBInfo.LAST_NAME},
				whereClause, null, null, null, null);
		cur.moveToFirst();
		Star star = new Star(cur.getString(0).trim(), cur.getString(1).trim(), cur.getString(2).trim());
		cur.close();
		return star;
	}

	private static Set<String> getDirectors(String correctDirector){
		Set<String> directorSet = new HashSet<String>();
		directorSet.add(correctDirector);
		//get three wrong answers
		String whereClause = DBInfo.DIRECTOR + " !=  " + correctDirector;
		Cursor cur = db.query(true, //distinct results
				DBInfo.MOVIES,
				new String[] {DBInfo.DIRECTOR},
				whereClause, null, null, null,null, (OPTION_NO - 1)+"");
		cur.moveToFirst();
		while(!cur.isAfterLast()){
			directorSet.add(cur.getString(0).trim());
			cur.moveToNext();
		}
		cur.close();
		return directorSet;
	}

	private static Set<String> getRandomYears(String correctYear){
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		Set<String> yearSet = new HashSet<String>();
		yearSet.add(correctYear);
		int min = Integer.parseInt(correctYear) - 10;
		while(yearSet.size() != OPTION_NO){
			int year = min + (int)(Math.random() * ((currentYear - min) + 1));
			yearSet.add(year+"");
		}
		return yearSet;
	}
}
