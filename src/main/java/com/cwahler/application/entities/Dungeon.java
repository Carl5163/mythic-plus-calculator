package com.cwahler.application.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.vaadin.flow.function.ValueProvider;


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
    private double fortPercentRemaining = 0;
    private double tyranPercentRemaining = 0;
    private double totalScore = 0;

	private int fortLevelActual = 0;
    private int tyranLevelActual = 0;
    private double fortActual = 0;
    private double tyranActual = 0;
    private double fortPercentRemainingActual = 0;
    private double tyranPercentRemainingActual = 0;
    private double totalActual = 0;
    
    private static double[] BASE = {	  0,    
    								  0, 40, 45, 55, 60, 65, 75, 80, 85,100,
    								105,110,115,120,125,130,135,140,145,150,
    								155,160,165,170,175,180,185,190,195,200};

    public Dungeon(){}
    
    public Dungeon(String name, int fortLevel, int tyranLevel, double fortScore, double tyranScore, long fortParTimeMs, long tyranParTimeMs, long fortClearTimeMs, long tyranClearTimeMs) {
        this.name = name;
        this.fortLevel = fortLevel;
        this.tyranLevel = tyranLevel;
        this.fortScore = fortScore;
        this.tyranScore = tyranScore;
        this.fortPercentRemaining = (((double)fortParTimeMs-(double)fortClearTimeMs)/(double)fortParTimeMs)*100;
        this.tyranPercentRemaining = (((double)tyranParTimeMs-(double)tyranClearTimeMs)/(double)tyranParTimeMs)*100;
        this.setTotalScore();
    }
    
    public void setActuals() {
        fortLevelActual = fortLevel;
        tyranLevelActual = tyranLevel;
        fortActual = fortScore;
        tyranActual = tyranScore;
        totalActual = totalScore;
        setFortPercentRemainingActual(fortPercentRemaining);
        setTyranPercentRemainingActual(tyranPercentRemaining);
    }
    
    public void update(double fortPercentRemaining, double tyranPercentRemaining) {
    	if(fortLevel > 30) {
    		fortScore = BASE[30] + 5*(fortLevel-30);
    	} else {
    		fortScore = BASE[fortLevel];
    	}
    	double bonus = 0;
    	if(fortPercentRemaining >= -40 && fortPercentRemaining < 0) {
    		bonus = (5d/40d)*fortPercentRemaining-5;
    	} else if(fortPercentRemaining > 0 && fortPercentRemaining <= 40) {
    		bonus = (5d/40d)*fortPercentRemaining;
    	} else if(fortPercentRemaining > 40) {
    		bonus = 5;
    	}
		fortScore += bonus;
		


    	if(tyranLevel > 30) {
    		tyranScore = BASE[30] + 5*(tyranLevel-30);
    	} else {
    		tyranScore = BASE[tyranLevel];
    	}
    	if(tyranPercentRemaining >= -40 && tyranPercentRemaining < 0) {
    		bonus = (5d/40d)*tyranPercentRemaining-5;
    	} else if(tyranPercentRemaining > 0 && tyranPercentRemaining <= 40) {
    		bonus = (5d/40d)*tyranPercentRemaining;
    	} else if(tyranPercentRemaining > 40) {
    		bonus = 5;
    	}
		tyranScore += bonus;
		
    	if(fortScore > tyranScore) {
    		fortScore *= 1.5;
    		tyranScore *= .5;
    	} else {
    		tyranScore *= 1.5;
    		fortScore *= .5;
    	}
    	setTotalScore();
    }
    
    public String getDeltaSignDouble(ValueProvider<Dungeon, Double> curProv, ValueProvider<Dungeon, Double> actProv) {
    	Double delta = curProv.apply(this) - actProv.apply(this);
    	int comparison = Double.compare(delta, 0);
    	if(comparison > 0) {
        	return "+";
    	}
    	return "";
    }
    
    public String getDeltaSignInt(ValueProvider<Dungeon, Integer> curProv, ValueProvider<Dungeon, Integer> actProv) {
    	Integer delta = curProv.apply(this) - actProv.apply(this);
    	int comparison = Integer.compare(delta, 0);
    	if(comparison > 0) {
        	return "+";
    	}
    	return "";
    }
    
    public String getDeltaColorDouble(ValueProvider<Dungeon, Double> curProv, ValueProvider<Dungeon, Double> actProv) {
    	Double delta = curProv.apply(this) - actProv.apply(this);
    	int comparison = Double.compare(delta, 0);
    	if(comparison < 0) {
        	return "red";
    	} else if(comparison > 0) {
        	return "lime";
    	} else {
        	return "white";
    	}
    }
    
    public String getDeltaColorInt(ValueProvider<Dungeon, Integer> curProv, ValueProvider<Dungeon, Integer> actProv) {
    	Integer delta = curProv.apply(this) - actProv.apply(this);
    	int comparison = Integer.compare(delta, 0);
    	if(comparison < 0) {
        	return "red";
    	} else if(comparison > 0) {
        	return "lime";
    	} else {
        	return "white";
    	}
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
    
    public double getFortPercentRemaining() {
        return fortPercentRemaining;
    }
    public void setFortPercentRemaining(long parTime, long clearTime) {
        this.fortPercentRemaining = (((double)parTime-(double)clearTime)/(double)parTime)*100;
    }
    public void setFortPercentRemaining(double percentRemaining) {
        this.fortPercentRemaining = percentRemaining;
    }

    public double getTyranPercentRemaining() {
        return tyranPercentRemaining;
    }
    public void setTyranPercentRemaining(long parTime, long clearTime) {
        this.tyranPercentRemaining = (((double)parTime-(double)clearTime)/(double)parTime)*100;
    }
    public void setTyranPercentRemaining(double percentRemaining) {
        this.tyranPercentRemaining = percentRemaining;
    }

    public void setTotalScore() {
        this.totalScore = fortScore + tyranScore;
    }

    public Long getId() {
        return id;
    }
    

    public int getFortLevelActual() {
		return fortLevelActual;
	}

	public void setFortLevelActual(int fortLevelActual) {
		this.fortLevelActual = fortLevelActual;
	}

	public int getTyranLevelActual() {
		return tyranLevelActual;
	}

	public void setTyranLevelActual(int tyranLevelActual) {
		this.tyranLevelActual = tyranLevelActual;
	}

	public double getFortActual() {
		return fortActual;
	}

	public void setFortActual(double fortActual) {
		this.fortActual = fortActual;
	}

	public double getTyranActual() {
		return tyranActual;
	}

	public void setTyranActual(double tyranActual) {
		this.tyranActual = tyranActual;
	}

	public double getTotalActual() {
		return totalActual;
	}

	public void setTotalActual(double totalActual) {
		this.totalActual = totalActual;
	}

    @Override
    public String toString() {
        return this.name;
    }

	public double getFortPercentRemainingActual() {
		return fortPercentRemainingActual;
	}

	public void setFortPercentRemainingActual(double fortPercentRemainingActual) {
		this.fortPercentRemainingActual = fortPercentRemainingActual;
	}

	public double getTyranPercentRemainingActual() {
		return tyranPercentRemainingActual;
	}

	public void setTyranPercentRemainingActual(double tyranPercentRemainingActual) {
		this.tyranPercentRemainingActual = tyranPercentRemainingActual;
	}
    
    
}