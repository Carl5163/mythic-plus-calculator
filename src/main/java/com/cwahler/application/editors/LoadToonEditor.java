package com.cwahler.application.editors;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

public class LoadToonEditor extends Dialog implements KeyNotifier {
	
	private static Logger logger = LoggerFactory.getLogger(LoadToonEditor.class);
	
	private static final String[] REGIONS = {
			"US",
			"EU",
			"KR",
			"TW"
	};
	private static Map<String, ArrayList<String>> REALMS = new HashMap<String, ArrayList<String>>();

	private ComboBox<String> region = new ComboBox<String>("Region");
	private ComboBox<String> realm = new ComboBox<String>("Realm");
	private TextField name = new TextField("Character Name");

	private Button loadButton;
	private Button cancelButton;
	
	public LoadToonEditor() {
		
		try {
			getAuthToken();
		} catch (RestClientException e1) {
			e1.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		

		region.setItems(REGIONS);
		region.setValue("US");
		realm.setItems(REALMS.get("US"));
		
		//TODO: Add value Changed listener
		//TODO Get the load code out of the dialog so we only load once

		Div titleBar = new Div();
        Span titleSpan = new Span("Load Character");
        titleSpan.getStyle().set("margin-left", "10px");
        titleBar.add(titleSpan);
        titleBar.getStyle().set("background", "#196cda");
        titleBar.getStyle().set("margin-left", "-24px");
        titleBar.getStyle().set("margin-bottom", "18px");
        titleBar.getStyle().set("width", "calc(100% + 48px)");
        titleBar.getStyle().set("margin-top", "-24px");
        titleBar.getStyle().set("color", "white");

        Icon i = new Icon(VaadinIcon.CLOSE);
        i.getElement().getStyle().set("float", "right");
        i.getElement().getStyle().set("width", "16px");
        i.getElement().getStyle().set("margin-right", "5px");
        i.getElement().getStyle().set("cursor", "pointer");
        
        titleBar.add(i);
        i.addClickListener(listener -> {
                close();
        });

        add(titleBar);

		//TODO: Remove this
		region.setValue("US");
		realm.setValue("Thunderlord");
		name.setValue("Gaulis");
		

		addKeyPressListener(Key.ENTER, e -> loadButton.click());
		
		loadButton = new Button("Confirm", VaadinIcon.CLOUD_DOWNLOAD.create());
		loadButton.addClickListener(e -> {
			close();
		});
		loadButton.getElement().getThemeList().add("primary");
		
		cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create());
		cancelButton.addClickListener(e -> {
			close();
		});

		HorizontalLayout actions = new HorizontalLayout(cancelButton, loadButton);
		VerticalLayout fields = new VerticalLayout(region, realm, name, actions);
		add(fields);
	}
	
	public void getAuthToken() throws RestClientException, ParseException {
		
		String authUri;
		authUri = "https://us.battle.net/oauth/token";
		RestTemplate restTemplate = new RestTemplate();
		String username = "7d06ac10b32343bc9e494dd9fef043cb";
		String password = "pyuitmsMxL53705E5z4FmSIZ3QYQ8uvd";
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authUri)
		        .queryParam("grant_type", "client_credentials");
		HttpEntity<String> request = new HttpEntity<String>(headers);
		URI myUri = builder.buildAndExpand().toUri();
		JSONObject response;
		response = (JSONObject)(new JSONParser().parse(restTemplate.exchange(myUri, HttpMethod.POST, request, String.class).getBody()));
		String token = (String) response.get("access_token");
		logger.info("TOKEN = " + token);
		

		for(String reg : REGIONS) {
			
			String lowerReg = reg.toLowerCase();
			
			restTemplate = new RestTemplate();
			String dataURI = "https://" + lowerReg + ".api.blizzard.com/data/wow/realm/index";
			headers = new HttpHeaders();
			headers.setBearerAuth(token);
			request = new HttpEntity<String>(headers);
			builder = UriComponentsBuilder.fromUriString(dataURI)
			        .queryParam("locale", "en_" + reg)
			        .queryParam("access_token", token)
			        .queryParam("namespace","dynamic-"+lowerReg);
			myUri = builder.buildAndExpand().toUri();
			logger.info(myUri.toString());
			response = (JSONObject)(new JSONParser().parse(restTemplate.exchange(myUri, HttpMethod.GET, request, String.class).getBody()));
			JSONArray realmArray = ((JSONArray)response.get("realms"));
			Iterator<JSONObject> itr = realmArray.iterator();
			
			ArrayList<String> regionList = new ArrayList<String>();
			while(itr.hasNext()) {
				JSONObject rjson = itr.next();
				String rName = (String)rjson.get("name");
				regionList.add(rName);
			}
			REALMS.put(reg, regionList);
		}
	}
	
	@Override
	public void open() {
		region.focus();
		super.open();
	}
	
	public String getRegion() {
		return region.getValue().trim();
	}
	public String getRealm() {
		return realm.getValue().trim();
	}
	public String getName() {
		return name.getValue().trim();
	}
	
	public void addOnLoadListener(ComponentEventListener<ClickEvent<Button>> e) {
		loadButton.addClickListener(e);
	}

}
