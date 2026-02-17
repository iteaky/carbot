package com.epam.carbot.view;

import com.epam.carbot.dto.chat.ChatMessage;
import com.epam.carbot.service.CarBotService;
import com.epam.carbot.service.ChatSessionService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Route("")
public class ChatView extends VerticalLayout {

    private final ChatSessionService sessions;
    private final CarBotService bot;

    private final Div messages = new Div();
    private final TextField input = new TextField();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public ChatView(ChatSessionService sessions, CarBotService bot) {
        this.sessions = sessions;
        this.bot = bot;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        Div header = new Div();
        header.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Border.BOTTOM, LumoUtility.Background.BASE);
        header.getStyle().set("position", "sticky");
        header.getStyle().set("top", "0");
        header.getStyle().set("z-index", "1");

        H2 title = new H2("Чат покупки авто");
        title.getStyle().set("margin", "0");

        Span status = new Span("Онлайн • Помогаю подобрать авто");
        status.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Div titleBlock = new Div(title, status);
        titleBlock.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        header.add(titleBlock);

        add(header);

        messages.setWidthFull();
        messages.addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.MEDIUM);
        messages.getStyle().set("max-width", "860px");
        messages.getStyle().set("margin", "0 auto");
        messages.getStyle().set("flex", "1 1 auto");
        add(messages);
        setFlexGrow(1, messages);

        input.setWidthFull();
        input.setPlaceholder("Напиши сообщение…");

        Button send = new Button("Send", e -> sendMessage());
        send.addClickShortcut(Key.ENTER);

        HorizontalLayout composer = new HorizontalLayout(input, send);
        composer.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Border.TOP, LumoUtility.Background.BASE);
        composer.setWidthFull();
        composer.setFlexGrow(1, input);
        add(composer);

        // показать диалог имени, если не введено
        ensureUsernameThenLoad();
    }

    private void ensureUsernameThenLoad() {
        if (sessions.username() == null || sessions.username().isBlank()) {
            Dialog d = new Dialog();
            d.setCloseOnEsc(false);
            d.setCloseOnOutsideClick(false);

            TextField name = new TextField("Ваше имя");
            name.setPlaceholder("Например: Konstantin");
            name.setWidthFull();

            Button ok = new Button("Начать", e -> {
                String n = name.getValue() == null ? "" : name.getValue().trim();
                if (n.isBlank()) {
                    name.setInvalid(true);
                    name.setErrorMessage("Введите имя");
                    return;
                }
                sessions.setUsername(n);
                d.close();

                // приветствие
                String sid = sessions.sessionId();
                sessions.addBotMessage(sid, "Привет, " + n + "! Помогу купить авто. Скажи бюджет и город/страну покупки.");
                renderAll();
            });

            ok.addClickShortcut(Key.ENTER);

            VerticalLayout content = new VerticalLayout(new Div(new com.vaadin.flow.component.html.Span("Введите имя, чтобы начать чат.")), name, ok);
            content.setWidth("420px");

            d.add(content);
            d.open();
        } else {
            renderAll();
        }
    }

    private void sendMessage() {
        String text = input.getValue();
        if (text == null || text.trim().isBlank()) return;

        String sid = sessions.sessionId();
        String username = sessions.username();

        sessions.addUserMessage(sid, username, text.trim());

        // ответ бота
        String answer = bot.reply(username, text.trim());
        sessions.addBotMessage(sid, answer);

        input.clear();
        renderAll();
        scrollToBottom();
    }

    private void renderAll() {
        messages.removeAll();
        String sid = sessions.sessionId();

        for (ChatMessage m : sessions.getHistory(sid)) {
            Div item = new Div();
            item.addClassNames(LumoUtility.Padding.Vertical.SMALL);

            Span author = new Span(m.author());
            author.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.End.SMALL);
            if (m.fromUser()) {
                author.getStyle().set("color", "var(--lumo-primary-text-color)");
            }

            String time = TIME_FORMAT.format(m.at().atZone(ZoneId.systemDefault()));
            Span timestamp = new Span(time);
            timestamp.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.XSMALL);

            Div meta = new Div(author, timestamp);
            meta.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.BASELINE);

            Span text = new Span(m.text());
            text.getStyle().set("white-space", "pre-wrap");

            Div body = new Div(text);
            body.addClassNames(LumoUtility.Padding.Top.XSMALL);

            item.add(meta, body);
            messages.add(item);
        }
    }

    private void scrollToBottom() {
        UI.getCurrent().getPage().executeJs(
                "const el = $0; el.scrollTop = el.scrollHeight;", messages.getElement()
        );
    }
}
