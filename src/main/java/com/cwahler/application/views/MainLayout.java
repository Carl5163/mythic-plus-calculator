package com.cwahler.application.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.cwahler.application.editors.DungeonEditor;
import com.cwahler.application.editors.LoadToonEditor;
import com.cwahler.application.entities.Dungeon;
import com.cwahler.application.repositories.DungeonRepository;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(name = "Mythic+ Calculator", shortName = "M+Calc", enableInstallPrompt = false)
@PageTitle("Mythic+ Calculator")
@Route(value = "")
@Theme(value = Lumo.class, variant = Lumo.DARK)
public class MainLayout extends VerticalLayout {

	private final DungeonRepository repo;

	final Grid<Dungeon> grid;
	
	String bannerUrl;
	String portraitUrl;
	String bannerUrlBase = "https://cdnassets.raider.io/images/profile/masthead_backdrops/v2/";

	private final DungeonEditor editor;
	
	private LoadToonEditor loadEditor;
	private Button loadButton;

	VerticalLayout banner;
	Image portrait;
	Label tName = new Label(""), tGuild = new Label(""), tRealm = new Label(""), tClass = new Label("");
	
	Column<Dungeon> tyranLevel;
	Column<Dungeon>  fortLevel;
	Column<Dungeon>  fortScore;
	Column<Dungeon>  tyranScore;
	Column<Dungeon>  totalScore;
	
	String toonName;
	String toonRegion;
	String toonServer;
	
	
	List<Dungeon> dungeonList;
	
	private static Logger logger = LoggerFactory.getLogger(MainLayout.class);

	public MainLayout(DungeonRepository repo) {
		
		this.repo = repo;
		this.grid = new Grid<>(Dungeon.class);
		dungeonList = new ArrayList<Dungeon>();
		repo.deleteAll();
		
		editor = new DungeonEditor(this.repo);

		loadButton = new Button("Load Character", VaadinIcon.CLOUD_DOWNLOAD.create());
		loadButton.addClickListener(e -> {
			loadEditor.open();
		});
		
		loadEditor = new LoadToonEditor();
		loadEditor.addOnLoadListener(e -> {
			loadAll(repo);
		});

		
		banner = new VerticalLayout();
		portrait = new Image("https://images-wixmp-ed30a86b8c4ca887773594c2.wixmp.com/f/5ab19f85-6c25-4d8d-a4d5-9e8070c3164c/d7sd1ab-9411e123-5bb8-4077-aa51-4b93f3ce1ac6.png?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1cm46YXBwOjdlMGQxODg5ODIyNjQzNzNhNWYwZDQxNWVhMGQyNmUwIiwiaXNzIjoidXJuOmFwcDo3ZTBkMTg4OTgyMjY0MzczYTVmMGQ0MTVlYTBkMjZlMCIsIm9iaiI6W1t7InBhdGgiOiJcL2ZcLzVhYjE5Zjg1LTZjMjUtNGQ4ZC1hNGQ1LTllODA3MGMzMTY0Y1wvZDdzZDFhYi05NDExZTEyMy01YmI4LTQwNzctYWE1MS00YjkzZjNjZTFhYzYucG5nIn1dXSwiYXVkIjpbInVybjpzZXJ2aWNlOmZpbGUuZG93bmxvYWQiXX0.XfeAR2CNe8JsSU2zUfcwP-85QRz5SormCKnuA3CMtpQ", "No Image");
		portrait.setWidth(185, Unit.PIXELS);
		portrait.setHeight(185, Unit.PIXELS);
		VerticalLayout infoPanel = new VerticalLayout();
		infoPanel.add(tName, tGuild, tRealm, tClass);
		HorizontalLayout actions = new HorizontalLayout(portrait, infoPanel);
		banner.add(actions, loadButton);
		banner.getStyle().set("background", "url(https://images.squarespace-cdn.com/content/v1/5db856944658bf0045b81003/1587676647505-FPJJUJNL0E2RSSZV5ASX/banner2-01.jpg?format=2000w)");
		banner.setSizeFull();
		add(banner);
		
		
		editor.setSaveHandler((dungeon, fortPercentRemaining, tyranPercentRemaining) -> {
			if(editor.isValid()) {
				updateDungeon(dungeon, fortPercentRemaining, tyranPercentRemaining);
				repo.save(dungeon);
				editor.close();
				listDungeons("");
			}
		});
		
		
		// build layout
		grid.setHeightByRows(true);
		add(grid);
		grid.setColumns("name");
		tyranLevel = grid.addColumn(getDeltaRendererInt("", "tyranLevel", Dungeon::getTyranLevel, Dungeon::getTyranLevelActual)).setHeader("Tyrannical Level");
		fortLevel = grid.addColumn(getDeltaRendererInt("", "fortLevel", Dungeon::getFortLevel, Dungeon::getFortLevelActual)).setHeader("Fortified Level");

		fortScore = grid.addColumn(getDeltaRendererDouble("", "fortScore", Dungeon::getFortScore, Dungeon::getFortActual)).setHeader("Fortified Score");
		tyranScore = grid.addColumn(getDeltaRendererDouble("", "tyranScore", Dungeon::getTyranScore, Dungeon::getTyranActual)).setHeader("Tyrannical  Score");
		totalScore = grid.addColumn(new NumberRenderer<>(Dungeon::getTotalScore, "%(,.1f", getLocale())).setHeader("Total Score");
		fortScore.setFooter("Total Fortified: " + String.format("%1$,.1f", 0d));
		tyranScore.setFooter("Total Tyrannical: " + String.format("%1$,.1f", 0d));
		totalScore.setFooter("Total Overall: " + String.format("%1$,.1f", 0d));
		grid.addComponentColumn(item -> {
				HorizontalLayout l = new HorizontalLayout();
				l.add(createEditButton(item), createRefreshButton(item, repo));
				l.setJustifyContentMode(JustifyContentMode.END);
				return l;
			}).setFooter(createRefreshAllButton(repo)).setTextAlign(ColumnTextAlign.END);
		
		// Initialize listing
		listDungeons(null);
	}
	
	private TemplateRenderer<Dungeon> getDeltaRendererInt(String prefix, String prop, ValueProvider<Dungeon, Integer> curProv, ValueProvider<Dungeon, Integer> actProv) {
		TemplateRenderer<Dungeon> ret = null;
		ret = TemplateRenderer.<Dungeon>of(prefix + "[[item."+prop+"]] <span style=\"color:[[item."+prop+"deltaColor]];\">([[item."+prop+"deltaSign]][[item."+prop+"delta]])</span>")
				.withProperty(prop, dungeon -> curProv.apply(dungeon))
				.withProperty(prop+"deltaColor", dungeon -> dungeon.getDeltaColorInt(curProv, actProv))
				.withProperty(prop+"deltaSign", dungeon -> dungeon.getDeltaSignInt(curProv, actProv))
				.withProperty(prop+"delta", dungeon -> curProv.apply(dungeon)-actProv.apply(dungeon));
		return ret;
	}
	
	private TemplateRenderer<Dungeon> getDeltaRendererDouble(String prefix, String prop, ValueProvider<Dungeon, Double> curProv, ValueProvider<Dungeon, Double> actProv) {
		TemplateRenderer<Dungeon> ret = null;
		ret = TemplateRenderer.<Dungeon>of(prefix + "[[item."+prop+"]] <span style=\"color:[[item."+prop+"deltaColor]];\">([[item."+prop+"deltaSign]][[item."+prop+"delta]])</span>")
				.withProperty(prop, dungeon -> String.format("%1$,.1f", curProv.apply(dungeon)))
				.withProperty(prop+"deltaColor", dungeon -> dungeon.getDeltaColorDouble(curProv, actProv))
				.withProperty(prop+"deltaSign", dungeon -> dungeon.getDeltaSignDouble(curProv, actProv))
				.withProperty(prop+"delta", dungeon -> String.format("%1$,.1f", curProv.apply(dungeon)-actProv.apply(dungeon)));
		return ret;
	}
	
	private void loadAll(DungeonRepository repo) {
		
		List<Dungeon> dungeons = getBestDungeons(loadEditor.getRegion(), loadEditor.getRealm(), loadEditor.getName());
		repo.deleteAll();
		
		this.toonName = loadEditor.getName();
		this.toonRegion = loadEditor.getRegion();
		this.toonServer = loadEditor.getRealm();
		if(dungeons != null) {
			repo.saveAll(dungeons);
			getAltDungeons(loadEditor.getRegion(), loadEditor.getRealm(), loadEditor.getName(), repo);
			listDungeons("");
		}
	}
	
	private Button createRefreshAllButton(DungeonRepository repo) {
	    @SuppressWarnings("unchecked")
	    Button button = new Button("Reload All", VaadinIcon.REFRESH.create(), clickEvent -> {
	    	if(this.toonName.length() > 0) {
	    		loadAll(repo);
	    	}
	    });

	    return button;
	}
	
	private Button createEditButton(Dungeon item) {
	    @SuppressWarnings("unchecked")
	    Button button = new Button(VaadinIcon.EDIT.create(), clickEvent -> {
	        if(item != null) {
				editor.open(item);
			}
	    });
	    return button;
	}
	private Button createRefreshButton(Dungeon item, DungeonRepository repo) {
	    @SuppressWarnings("unchecked")
	    Button button = new Button(VaadinIcon.REFRESH.create(), clickEvent -> {
	        refreshDungeon(item);
			repo.save(item);
			listDungeons("");
	    });
	    return button;
	}
	
	public void getTotals() {
		double total = 0;
		double totalFort = 0;
		double totalTyran = 0;
		double averageFort = 0;
		double averageTyran = 0;
		double totalActual = 0;
		double totalFortActual = 0;
		double totalTyranActual = 0;
		double averageFortActual = 0;
		double averageTyranActual = 0;
		for(Dungeon d : dungeonList) {
			total += d.getTotalScore();
			totalFort += d.getFortScore();
			totalTyran += d.getTyranScore();
			averageFort += d.getFortLevel();
			averageTyran += d.getTyranLevel();

			totalActual += d.getTotalActual();
			totalFortActual += d.getFortActual();
			totalTyranActual += d.getTyranActual();
			averageFortActual += d.getFortLevelActual();
			averageTyranActual += d.getTyranLevelActual();
		}
		

		averageFort /= 8;		
		averageTyran /= 8;

		averageFortActual /= 8;
		averageTyranActual /= 8;
		
		String fortTotalColor = totalFort-totalFortActual > 0 ? "lime" : "red";
		if(doubleCompare(totalFort-totalFortActual, 0) == 0) fortTotalColor = "white";
		String fortTotalSign = totalFort-totalFortActual > 0 ? "+" : "";
		String averageFortColor = averageFort-averageFortActual > 0 ? "lime" : "red";
		if(doubleCompare(averageFort-averageFortActual, 0) == 0) averageFortColor = "white";
		String averageFortSign = averageFort-averageFortActual > 0 ? "+" : "";

		String tyranTotalColor = totalTyran-totalTyranActual > 0 ? "lime" : "red";
		if(doubleCompare(totalTyran-totalTyranActual, 0) == 0) tyranTotalColor = "white";
		String tyranTotalSign = totalTyran-totalTyranActual > 0 ? "+" : "";
		String averageTyranColor = averageTyran-averageTyranActual > 0 ? "lime" : "red";
		if(doubleCompare(averageTyran-averageTyranActual, 0) == 0) averageTyranColor = "white";
		String averageTyranSign = averageTyran-averageTyranActual > 0 ? "+" : "";
		
		String averageTotalColor = total-totalActual > 0 ? "lime" : "red";
		if(doubleCompare(total-totalActual, 0) == 0) averageTotalColor = "white";
		String averageTotalSign = total-totalActual > 0 ? "+" : "";
		
		Html fortLevelRenderer = new Html("<p><span>Average Fortified: </span>" + String.format("%1$,.1f", averageFort) +" <span style=\"color:" + averageFortColor +";\">("+averageFortSign+String.format("%1$,.1f", (averageFort-averageFortActual))+")</span></p>");
		Html tyranLevelRenderer = new Html("<p><span>Average Tyrannical: </span>" + String.format("%1$,.1f", averageTyran) +" <span style=\"color:" + averageTyranColor +";\">("+averageTyranSign+String.format("%1$,.1f", (averageTyran-averageTyranActual))+")</span></p>");
		Html fortScoreRenderer = new Html("<p><span>Total Fortified: </span>" + String.format("%1$,.1f", totalFort) +" <span style=\"color:" + fortTotalColor +";\">("+fortTotalSign+String.format("%1$,.1f", (totalFort-totalFortActual))+")</span></p>");
		Html tyranScoreRenderer = new Html("<p><span>Total Tyrannical: </span>" + String.format("%1$,.1f", totalTyran) +" <span style=\"color:" + tyranTotalColor +";\">("+tyranTotalSign+String.format("%1$,.1f", (totalTyran-totalTyranActual))+")</span></p>");
		
		Html totalScoreRenderer = new Html("<p><span>Total Overall: </span>" + String.format("%1$,.1f", total) +" <span style=\"color:" + averageTotalColor +";\">("+averageTotalSign+String.format("%1$,.1f", (total-totalActual))+")</span></p>");
		
		fortLevel.setFooter(fortLevelRenderer);
		tyranLevel.setFooter(tyranLevelRenderer);
		fortScore.setFooter(fortScoreRenderer);
		tyranScore.setFooter(tyranScoreRenderer);
		totalScore.setFooter(totalScoreRenderer);
	}

	public int doubleCompare(double d1, double d2) {
		double epsilon = 0.05d;
		int ret = 0;
		if(d1 < d2-epsilon) {
			ret = -1;
		}
		if(d1 > d2+epsilon) {
			ret = 1;
		}
		if(Math.abs(d1 - d2) <= epsilon) {
			ret = 0;
		}
		return ret;
	}
	
	// tag::listDungeons[]
	void listDungeons(String filterText) {
		if (!StringUtils.hasLength(filterText)) {
			dungeonList = repo.findAll();
			grid.setItems(dungeonList);
		}
		else {
			dungeonList = repo.findByNameStartsWithIgnoreCase(filterText);
			grid.setItems(dungeonList);
		}
		getTotals();
	}
	// end::listDungeons[]

	private void updateDungeon(Dungeon dungeon, Double fortPercentRemaining, Double tyranPercentRemaining) {
		dungeon.update(fortPercentRemaining, tyranPercentRemaining);
	}
	
	private void refreshDungeon(Dungeon dungeon) {
		final String bestURI = "https://raider.io/api/v1/characters/profile?region=" + toonRegion + "&realm=" + toonServer + "&name=" + toonName + "&fields=mythic_plus_best_runs";
		final String altURI = "https://raider.io/api/v1/characters/profile?region=" + toonRegion + "&realm=" + toonServer + "&name=" + toonName + "&fields=mythic_plus_alternate_runs";
		RestTemplate fortTemplate = new RestTemplate();
		RestTemplate tyranTemplate = new RestTemplate();

		try {
			JSONObject bestJO = (JSONObject)(new JSONParser().parse(fortTemplate.getForObject(bestURI, String.class)));
			JSONObject altJO = (JSONObject)(new JSONParser().parse(tyranTemplate.getForObject(altURI, String.class)));
			
					
			JSONArray bestArray = ((JSONArray)bestJO.get("mythic_plus_best_runs"));
			JSONArray altArray = ((JSONArray)altJO.get("mythic_plus_alternate_runs"));
			
	
			Iterator<JSONObject> itr = bestArray.iterator();
			String affix = "Fortified";
			Double factor = 1.5;
			
			for(int i = 0; i < 2; i++) {
				while(itr.hasNext()) {
					JSONObject djson = (JSONObject) itr.next();
					if(((String)djson.get("dungeon")).equals(dungeon.getName())) {
						JSONArray affixArray = ((JSONArray)djson.get("affixes"));
						
						if(((String)(((JSONObject)affixArray.get(0)).get("name"))).equals(affix)) {
							dungeon.setFortLevel(Integer.parseInt(djson.get("mythic_level").toString()));
							dungeon.setFortScore(Double.parseDouble(djson.get("score").toString())*factor);
							dungeon.setFortPercentRemaining(Long.parseLong(djson.get("par_time_ms").toString()), Long.parseLong(djson.get("clear_time_ms").toString()));
						} else {
							dungeon.setTyranLevel(Integer.parseInt(djson.get("mythic_level").toString()));
							dungeon.setTyranScore(Double.parseDouble(djson.get("score").toString())*factor);
							dungeon.setTyranPercentRemaining(Long.parseLong(djson.get("par_time_ms").toString()), Long.parseLong(djson.get("clear_time_ms").toString()));
						}
					}
				}
				itr = altArray.iterator();
				factor = 0.5;
			}

			dungeon.setTotalScore();
			notification("Refresh Successful!", "success");

		} catch (RestClientException e) {
			notification("Lookup Failed", "error");
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void notification(String message, String theme) {
		Span s = new Span(message);
		Notification n = new Notification(s);
		n.getElement().getThemeList().add(theme);
		n.setDuration(2000);
		n.setPosition(Notification.Position.TOP_CENTER);
		n.open();
	}
	
	
	private List<Dungeon> getBestDungeons(String region, String realm, String name) {
		List<Dungeon> dungeons = new ArrayList<Dungeon>();
		final String dungeonUri = "https://raider.io/api/v1/characters/profile?region=" + region + "&realm=" + realm + "&name=" + name + "&fields=mythic_plus_best_runs";
		final String guildUri = "https://raider.io/api/v1/characters/profile?region=" + region + "&realm=" + realm + "&name=" + name + "&fields=guild";
		RestTemplate restTemplate = new RestTemplate();

		try {
			JSONObject jo = (JSONObject)(new JSONParser().parse(restTemplate.getForObject(dungeonUri, String.class)));
			JSONObject guildJO = (JSONObject)(new JSONParser().parse(restTemplate.getForObject(guildUri, String.class)));
			
			JSONObject guildObject = (JSONObject)guildJO.get("guild");
			

			portraitUrl = (String) jo.get("thumbnail_url");
			bannerUrl = bannerUrlBase + (String) jo.get("profile_banner") + ".jpg";
			banner.getStyle().set("background", "url("+bannerUrl+")");
			portrait.setSrc(portraitUrl);
			portrait.setVisible(true);

			tName.setText((String) jo.get("name"));
			tGuild.setText("<" + (String) guildObject.get("name")+ ">");
			tRealm.setText("(" + ((String) jo.get("region")).toUpperCase() + ")" + (String) jo.get("realm"));
			tClass.setText((String) jo.get("race") + " " + (String) jo.get("active_spec_name") + " " + (String) jo.get("class"));
					
			JSONArray dungArray = ((JSONArray)jo.get("mythic_plus_best_runs"));
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> itr = dungArray.iterator();
			while(itr.hasNext()) {
				JSONObject djson = (JSONObject) itr.next();

				JSONArray affixArray = ((JSONArray)djson.get("affixes"));
				Dungeon d = new Dungeon();
				if(((String)(((JSONObject)affixArray.get(0)).get("name"))).equals("Fortified")) {
					d = new Dungeon(
					(String)djson.get("dungeon"), 
					Integer.parseInt(djson.get("mythic_level").toString()), 
					0, 
					Double.parseDouble(djson.get("score").toString())*1.5,
					0,
					Long.parseLong(djson.get("par_time_ms").toString()),
					1,
					Long.parseLong(djson.get("clear_time_ms").toString()),
					1);
				} else {
					d = new Dungeon(
					(String)djson.get("dungeon"), 
					0, 
					Integer.parseInt(djson.get("mythic_level").toString()), 
					0,
					Double.parseDouble(djson.get("score").toString())*1.5,
					1,
					Long.parseLong(djson.get("par_time_ms").toString()),
					1,
					Long.parseLong(djson.get("clear_time_ms").toString()));
				}

				
				dungeons.add(d);
			}

			notification("Refresh Successful!", "success");
			
		} catch (RestClientException e) {
			notification("Lookup Failed", "error");
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}


		return dungeons;
	}

	private void getAltDungeons(String region, String realm, String name, DungeonRepository repository) {
		final String uri = "https://raider.io/api/v1/characters/profile?region=" + region + "&realm=" + realm + "&name=" + name + "&fields=mythic_plus_alternate_runs";
		RestTemplate restTemplate = new RestTemplate();
		try {
			JSONObject jo = (JSONObject)(new JSONParser().parse(restTemplate.getForObject(uri, String.class)));
			JSONArray dungArray = ((JSONArray)jo.get("mythic_plus_alternate_runs"));
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> itr = dungArray.iterator();
			while(itr.hasNext()) {
				JSONObject djson = (JSONObject) itr.next();
				String dName = (String)djson.get("dungeon");
				List<Dungeon> queryResult = repository.findByNameStartsWithIgnoreCase(dName);
				if(queryResult.size() > 0) {
					Dungeon dBest = queryResult.get(0);

					JSONArray affixArray = ((JSONArray)djson.get("affixes"));
					if(((String)(((JSONObject)affixArray.get(0)).get("name"))).equals("Fortified")) {
						dBest.setFortLevel((Integer.parseInt(djson.get("mythic_level").toString())));
						dBest.setFortScore(Double.parseDouble(djson.get("score").toString())*.5);
						dBest.setFortPercentRemaining(Long.parseLong(djson.get("par_time_ms").toString()), Long.parseLong(djson.get("clear_time_ms").toString()));
					} else {
						dBest.setTyranLevel(Integer.parseInt(djson.get("mythic_level").toString()));
						dBest.setTyranScore(Double.parseDouble(djson.get("score").toString())*.5);
						dBest.setTyranPercentRemaining(Long.parseLong(djson.get("par_time_ms").toString()), Long.parseLong(djson.get("clear_time_ms").toString()));
					}
					dBest.setActuals();
					repository.save(dBest);
				}
			}
		} catch (RestClientException e) {
			notification("Lookup Failed", "error");
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
