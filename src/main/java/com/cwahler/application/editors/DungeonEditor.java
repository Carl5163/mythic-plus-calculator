package com.cwahler.application.editors;

import org.springframework.beans.factory.annotation.Autowired;

import com.cwahler.application.entities.Dungeon;
import com.cwahler.application.repositories.DungeonRepository;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;

public class DungeonEditor extends Dialog implements KeyNotifier {

	private final DungeonRepository repository;

	/**
	 * The currently edited dungeon
	 */
	private Dungeon dungeon;

	/* Fields to edit properties in Dungeon entity */
	TextField fortLevel = new TextField("fortLevel");
	TextField tyranLevel = new TextField("tyranLevel");
	TextField percentRemaining = new TextField("percentRemaining");

	RadioButtonGroup<String> rbg = new RadioButtonGroup<String>();
	
	/* Action buttons */
	
	Button save = new Button("Save", VaadinIcon.CHECK.create());
	Button cancel = new Button("Cancel");
	HorizontalLayout actions = new HorizontalLayout(save, cancel);
	Span titleSpan;

	Binder<Dungeon> binder = new Binder<>(Dungeon.class);
	private ChangeHandler changeHandler;
	
	@Autowired
	public DungeonEditor(DungeonRepository repository) {
		
		VerticalLayout layout = new VerticalLayout();
		
		Div titleBar = new Div();
        titleSpan = new Span("Edit Dungeon - ");
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
		
		this.repository = repository;
		layout.setWidth(this.getMaxWidth());
		Span pRem = new Span("Percentage Remaining (Negative for Overtime, Positive for Undertime)");
		percentRemaining.setLabel("");
		percentRemaining.setValue("0");
		

		rbg.setLabel("Affix");
		rbg.setItems("Fortified", "Tyranical");
		rbg.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		rbg.setValue("Fortified");
		rbg.addValueChangeListener(event -> {
			if(event.getValue().equals("Fortified")) {
				tyranLevel.setVisible(false);
				fortLevel.setVisible(true);
			} else {
				tyranLevel.setVisible(true);
				fortLevel.setVisible(false);
			}
		});
		
		fortLevel.setLabel("Fortified Level");
		tyranLevel.setLabel("Tyranical Level");
		tyranLevel.setVisible(false);
		
		layout.add(fortLevel, tyranLevel, rbg, pRem, percentRemaining, actions);


		binder.forField ( this.fortLevel )
        .withNullRepresentation ( "" )
        .withConverter ( new StringToIntegerConverter ( Integer.valueOf ( 0 ), "integers only" ) )
        .bind ( Dungeon::getFortLevel, Dungeon::setFortLevel );
		binder.forField ( this.tyranLevel )
        .withNullRepresentation ( "" )
        .withConverter ( new StringToIntegerConverter ( Integer.valueOf ( 0 ), "integers only" ) )
        .bind ( Dungeon::getTyranLevel, Dungeon::setTyranLevel );
		binder.forField ( this.percentRemaining )
        .withNullRepresentation ( "" )
        .withConverter ( new StringToDoubleConverter ( Double.valueOf ( 0 ), "doubles only" ) )
        .bind ( Dungeon::getPercentRemaining, Dungeon::setPercentRemaining );
		// bind using naming convention
		binder.bindInstanceFields(this);

		save.getElement().getThemeList().add("primary");

		addKeyPressListener(Key.ENTER, e -> save());

		add(layout);
		
		// wire action buttons to save, delete
		save.addClickListener(e -> save());
		cancel.addClickListener(e -> close());
	}
	
	public void open(Dungeon d) {
		titleSpan.setText("Edit Dungeon - " + d.getName());
		editDungeon(d);
		rbg.setValue("Fortified");
		super.open();
	}

	void save() {
		repository.save(dungeon);
		changeHandler.onSave(dungeon, rbg.getValue(), Double.parseDouble(percentRemaining.getValue()));
	}

	public interface ChangeHandler {
		void onSave(Dungeon dungeon, String affix, Double percent);
	}

	public final void editDungeon(Dungeon c) {
		if (c == null) {
			close();
			return;
		}
			dungeon = c;

		// Bind dungeon properties to similarly named fields
		// Could also use annotation or "manual binding" or programmatically
		// moving values from fields to entities before saving
		
		binder.setBean(dungeon);

		// Focus first name initially
		fortLevel.focus();
	}

	public void setSaveHandler(ChangeHandler h) {
		// ChangeHandler is notified when either save or delete
		// is clicked
		changeHandler = h;
	}

}