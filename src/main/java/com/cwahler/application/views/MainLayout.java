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
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
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
	
	List<Dungeon> dungeonList;
	
	private static Logger logger = LoggerFactory.getLogger(MainLayout.class);

	public MainLayout(DungeonRepository repo) {
		
		this.repo = repo;
		this.grid = new Grid<>(Dungeon.class);
		dungeonList = new ArrayList<Dungeon>();
		
		editor = new DungeonEditor(this.repo);

		loadButton = new Button("Load Character", VaadinIcon.CLOUD_DOWNLOAD.create());
		loadButton.addClickListener(e -> {
			loadEditor.open();
		});
		
		loadEditor = new LoadToonEditor();
		loadEditor.addOnLoadListener(e -> {
			List<Dungeon> dungeons = getBestDungeons(loadEditor.getRegion(), loadEditor.getRealm(), loadEditor.getName());
			
			if(dungeons != null) {
				repo.deleteAll();
				repo.saveAll(dungeons);
				getAltDungeons(loadEditor.getRegion(), loadEditor.getRealm(), loadEditor.getName(), repo);
				listDungeons("");
			}
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
		
		grid.asSingleSelect().addValueChangeListener(e -> {
			if(e != null) {
				if(e.getValue() != null) {
					editor.open(e.getValue());
				}
			}
		});
		
		editor.setSaveHandler((dungeon, affix, percentRemaining) -> {
			if(editor.isValid()) {
				updateDungeon(dungeon, affix, percentRemaining);
				repo.save(dungeon);
				editor.close();
				listDungeons("");
			}
		});
		
		grid.addComponentColumn(item -> createRemoveButton(grid, item))
        .setHeader("Actions");
		
		// build layout
		add(grid);

		grid.setColumns("name");
		tyranLevel = grid.addColumn("tyranLevel").setHeader("Tyranical Level");
		fortLevel = grid.addColumn("fortLevel").setHeader("Fortified Level");
		fortScore = grid.addColumn(new NumberRenderer<>(Dungeon::getFortScore, "%(,.1f", getLocale())).setHeader("Fortified Score");
		tyranScore = grid.addColumn(new NumberRenderer<>(Dungeon::getTyranScore, "%(,.1f", getLocale())).setHeader("Tyranical Score");
		totalScore = grid.addColumn(new NumberRenderer<>(Dungeon::getTotalScore, "%(,.1f", getLocale())).setHeader("Total Score");
		fortScore.setFooter("Total Fortified: " + String.format("%1$,.1f", 0d));
		tyranScore.setFooter("Total Tyranical: " + String.format("%1$,.1f", 0d));
		totalScore.setFooter("Total Overall: " + String.format("%1$,.1f", 0d));
		
		// Initialize listing
		listDungeons(null);
	}
	
	private Button createRemoveButton(Grid<Dungeon> grid, Dungeon item) {
	    @SuppressWarnings("unchecked")
	    Button button = new Button(VaadinIcon.EDIT.create(), clickEvent -> {
	        ListDataProvider<Dungeon> dataProvider = (ListDataProvider<Dungeon>) grid
	                .getDataProvider();
	        dataProvider.getItems().remove(item);
	        dataProvider.refreshAll();
	    });
	    return button;
	}
	
	public void getTotals() {
		double total = 0;
		double totalFort = 0;
		double totalTyran = 0;
		double averageFort = 0;
		double averageTyran = 0;
		for(Dungeon d : dungeonList) {
			total += d.getTotalScore();
			totalFort += d.getFortScore();
			totalTyran += d.getTyranScore();
			averageFort += d.getFortLevel();
			averageTyran += d.getTyranLevel();
		}
		fortLevel.setFooter("Average Fortified: " + String.format("%1$,.1f", averageFort/8d));
		tyranLevel.setFooter("Average Tyranical: " + String.format("%1$,.1f", averageTyran/8d));
		fortScore.setFooter("Total Fortified: " + String.format("%1$,.1f", totalFort));
		tyranScore.setFooter("Total Tyranical: " + String.format("%1$,.1f", totalTyran));
		totalScore.setFooter("Total Overall: " + String.format("%1$,.1f", total));
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

	private void updateDungeon(Dungeon dungeon, String affix, Double percentRemaining) {
		dungeon.update(affix, percentRemaining);
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
					((Long)djson.get("mythic_level")).intValue(), 
					0, 
					((Double)djson.get("score"))*1.5,
					0);
				} else {
					d = new Dungeon(
					(String)djson.get("dungeon"), 
					0, 
					((Long)djson.get("mythic_level")).intValue(), 
					0,
					((Double)djson.get("score"))*1.5);
				}

				
				dungeons.add(d);
			}

			Span s = new Span("Lookup Successful!");
			Notification n = new Notification(s);
			n.getElement().getThemeList().add("success");
			n.setDuration(2000);
			n.setPosition(Notification.Position.TOP_CENTER);
			n.open();

		} catch (RestClientException e) {
			Span s = new Span("Lookup Failed");
			Notification n = new Notification(s);
			n.getElement().getThemeList().add("error");
			n.setDuration(2000);
			n.setPosition(Notification.Position.TOP_CENTER);
			n.open();
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
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
						dBest.setFortLevel(((Long)djson.get("mythic_level")).intValue());
						dBest.setFortScore(((Number)djson.get("score")).doubleValue()*.5);
					} else {
						dBest.setTyranLevel(((Long)djson.get("mythic_level")).intValue());
						dBest.setTyranScore(((Number)djson.get("score")).doubleValue()*.5);
					}
					repository.save(dBest);
				}
			}
		} catch (RestClientException e) {
			Span s = new Span("Lookup Failed");
			Notification n = new Notification(s);
			n.getElement().getThemeList().add("error");
			n.setDuration(2000);
			n.setPosition(Notification.Position.TOP_CENTER);
			n.open();
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
