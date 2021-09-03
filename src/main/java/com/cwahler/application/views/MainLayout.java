package com.cwahler.application.views;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.cwahler.application.editors.DungeonEditor;
import com.cwahler.application.entities.Dungeon;
import com.cwahler.application.repositories.DungeonRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.Route;



@Route
public class MainLayout extends VerticalLayout {

	private final DungeonRepository repo;

	final Grid<Dungeon> grid;

	TextField region = new TextField("Region");
	TextField realm = new TextField("Realm");
	TextField name = new TextField("Character Name");

	private final DungeonEditor editor;
	

	private Button reloadButton;

	// private final Button addNewBtn;

	public MainLayout(DungeonRepository repo, DungeonEditor editor) {
		this.repo = repo;
		this.grid = new Grid<>(Dungeon.class);
		this.editor = editor;

		//TODO: Remove this
		region.setValue("us");
		realm.setValue("thunderlord");
		name.setValue("gaulis");
		
		
		HorizontalLayout actions = new HorizontalLayout(region, realm, name);
		add(actions);
		
		grid.asSingleSelect().addValueChangeListener(e -> {
			editor.editDungeon(e.getValue());
		});
		
		editor.setChangeHandler((dungeon, affix, percentRemaining) -> {
			updateDungeon(dungeon, affix, percentRemaining);
			repo.save(dungeon);
			editor.setVisible(false);
			listDungeons("");
		});
		

		this.reloadButton = new Button("Load Actual Scores", VaadinIcon.REFRESH.create());
		reloadButton.addClickListener(e -> {
			// save a couple of dungeons
			repo.deleteAll();
			repo.saveAll(getBestDungeons(region.getValue().trim(), realm.getValue().trim(), name.getValue().trim()));
			getAltDungeons(region.getValue().trim(), realm.getValue().trim(), name.getValue().trim(), repo);
			listDungeons("");
		});
		add(reloadButton);

		// build layout
		add(grid, editor);

		grid.setColumns("name", "fortLevel", "tyranLevel");
		grid.addColumn(new NumberRenderer<>(Dungeon::getFortScore, "%(,.1f", getLocale())).setHeader("Fortified Score");
		grid.addColumn(new NumberRenderer<>(Dungeon::getTyranScore, "%(,.1f", getLocale())).setHeader("Tyranical Score");
		grid.addColumn(new NumberRenderer<>(Dungeon::getTotalScore, "%(,.1f", getLocale())).setHeader("Total Score");
//		grid.addColumn(new NumberRenderer<>(Dungeon::getPercentRemaining, "%.1f%%", getLocale())).setHeader("Percent Remaining");

		// Initialize listing
		listDungeons(null);
	}

	// tag::listDungeons[]
	void listDungeons(String filterText) {
		if (!StringUtils.hasLength(filterText)) {
			grid.setItems(repo.findAll());
		}
		else {
			grid.setItems(repo.findByNameStartsWithIgnoreCase(filterText));
		}
	}
	// end::listDungeons[]

	private void updateDungeon(Dungeon dungeon, String affix, Double percentRemaining) {
		dungeon.update(affix, percentRemaining);
	}
	
	private List<Dungeon> getBestDungeons(String region, String realm, String name) {
		List<Dungeon> dungeons = new ArrayList<Dungeon>();
		final String uri = "https://raider.io/api/v1/characters/profile?region=" + region + "&realm=" + realm + "&name=" + name + "&fields=mythic_plus_best_runs";
		RestTemplate restTemplate = new RestTemplate();

		try {
			JSONObject jo = (JSONObject)(new JSONParser().parse(restTemplate.getForObject(uri, String.class)));
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
		} catch (ParseException e) {
			e.printStackTrace();
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
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
