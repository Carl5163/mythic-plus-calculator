package com.cwahler.application.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class Dungeon {

    @Id
    @GeneratedValue
    private Long id;

    private String name = "";
    private int fortLevel = 0;
    private int tyranLevel = 0;
    private double fortScore = 0;
    private double tyranScore = 0;
    private double totalScore = 0;
    private double percentRemaining = 0;
    
    public static double[] BASE = {	  0,    
    								  0, 40, 45, 55, 60, 65, 75, 80, 85,100,
    								105,110,115,120,125,130,135,140,145,150,
    								155,160,165,170,175,180,185,190,195,200};

    public Dungeon(){}

    public Dungeon(String name, int fortLevel, int tyranLevel, double fortScore, double tyranScore) {
        this.name = name;
        this.fortLevel = fortLevel;
        this.tyranLevel = tyranLevel;
        this.fortScore = fortScore;
        this.tyranScore = tyranScore;
        this.setTotalScore();
    }
    
    public void update(String affix, double percentRemaining) {
    	fortScore = BASE[fortLevel];
    	tyranScore = BASE[tyranLevel];
    	double bonus = 0;
    	if(percentRemaining >= -40 && percentRemaining < 0) {
    		bonus = (15d/40d)*percentRemaining;
    	} else if(percentRemaining > 0 && percentRemaining <= 40) {
    		bonus = (7.5d/40d)*percentRemaining;
    	}
    	
    	if(affix.equals("Fortified")) {
    		fortScore += bonus;
    	} else {
    		tyranScore += bonus;
    	}
    	if(fortScore > tyranScore) {
    		fortScore *= 1.5;
    		tyranScore *= .5;
    	} else {
    		tyranScore *= 1.5;
    		fortScore *= .5;
    	}
    	setTotalScore();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFortLevel() {
        return this.fortLevel;
    }

    public void setFortLevel(int level) {
        this.fortLevel = level;
    }

    public int getTyranLevel() {
        return this.tyranLevel;
    }

    public void setTyranLevel(int tyranLevel) {
        this.tyranLevel = tyranLevel;
    }

    public double getFortScore() {
        return this.fortScore;
    }

    public void setFortScore(double fortScore) {
        this.fortScore = fortScore;
        setTotalScore();
    }

    public double getTyranScore() {
        return this.tyranScore;
    }

    public void setTyranScore(double tyranScore) {
        this.tyranScore = tyranScore;
        setTotalScore();
    }

    public Double getTotalScore() {
        return this.totalScore;
    }
    
    public double getPercentRemaining() {
        return percentRemaining;
    }
    public void setPercentRemaining(double percentRemaining) {
        this.percentRemaining = percentRemaining;
    }

    public void setTotalScore() {
        // this.totalScore = Math.max(fortScore, tyranScore) + 0.5* Math.min(fortScore, tyranScore);
        this.totalScore = fortScore + tyranScore;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.name;
    }
    
    
}