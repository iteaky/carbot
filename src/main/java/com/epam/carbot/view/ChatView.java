package com.epam.carbot.view;

import com.epam.carbot.domain.BotReply;
import com.epam.carbot.dto.chat.ChatMessage;
import com.epam.carbot.service.CarBotService;
import com.epam.carbot.service.ChatSessionService;
import com.vaadin.flow.component.dependency.CssImport;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route("")
@CssImport("./styles/chat-view.css")
public class ChatView extends VerticalLayout {

    private final ChatSessionService sessions;
    private final CarBotService bot;

    private final Div messages = new Div();
    private final TextField input = new TextField();
    private final Button sendButton;

    private Div typingIndicator;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public ChatView(ChatSessionService sessions, CarBotService bot) {
        this.sessions = sessions;
        this.bot = bot;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("tg-chat-root");

        Div header = new Div();
        header.addClassName("tg-chat-header");

        H2 title = new H2("Чат покупки авто");
        title.addClassName("tg-chat-title");

        Span status = new Span("Онлайн • Помогаю подобрать авто");
        status.addClassName("tg-chat-status");

        Div titleBlock = new Div(title, status);
        titleBlock.addClassName("tg-chat-header-block");
        header.add(titleBlock);

        add(header);

        messages.setWidthFull();
        messages.addClassName("tg-chat-messages");
        add(messages);
        setFlexGrow(1, messages);

        input.setWidthFull();
        input.setPlaceholder("Напиши сообщение…");
        input.addClassName("tg-chat-input");

        sendButton = new Button("Отправить", e -> sendMessage());
        sendButton.addClassName("tg-chat-send");
        sendButton.addClickShortcut(Key.ENTER);

        HorizontalLayout composer = new HorizontalLayout(input, sendButton);
        composer.addClassName("tg-chat-composer");
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
                sessions.setPendingField(sid, "budget");
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
        String userText = text.trim();

        sessions.addUserMessage(sid, username, userText);

        String pendingField = sessions.pendingField(sid);
        List<ChatMessage> recentHistory = sessions.getRecentHistory(sid, 6);

        input.clear();
        setComposerEnabled(false);
        renderAll();
        showTypingIndicator();
        scrollToBottom();

        UI ui = UI.getCurrent();
        CompletableFuture
                .supplyAsync(() -> bot.reply(sid, username, userText, recentHistory, pendingField))
                .whenComplete((answer, error) -> ui.access(() -> {
                    hideTypingIndicator();

                    if (error != null) {
                        sessions.addBotMessage(sid, "Сервис временно недоступен. Попробуйте позже.");
                    } else {
                        sessions.setPendingField(sid, answer.pendingField());
                        sessions.addBotMessage(sid, answer.text());
                    }

                    renderAll();
                    scrollToBottom();
                    setComposerEnabled(true);
                    input.focus();
                }));
    }

    private void renderAll() {
        messages.removeAll();
        typingIndicator = null;
        String sid = sessions.sessionId();

        for (ChatMessage m : sessions.getHistory(sid)) {
            Div item = new Div();
            item.addClassName("tg-row");
            item.addClassName(m.fromUser() ? "tg-row-user" : "tg-row-bot");

            Span author = new Span(m.author());
            author.addClassName("tg-author");

            String time = TIME_FORMAT.format(m.at().atZone(ZoneId.systemDefault()));
            Span timestamp = new Span(time);
            timestamp.addClassName("tg-time");

            Div meta = new Div(author, timestamp);
            meta.addClassName("tg-meta");

            Span text = new Span(m.text());
            text.addClassName("tg-text");

            Div bubble = new Div(meta, text);
            bubble.addClassName("tg-bubble");
            bubble.addClassName(m.fromUser() ? "tg-user" : "tg-bot");

            item.add(bubble);
            messages.add(item);
        }
    }

    private void showTypingIndicator() {
        Div row = new Div();
        row.addClassNames("tg-row", "tg-row-bot", "tg-typing-row");

        Span author = new Span("AutoBot");
        author.addClassName("tg-author");

        String time = TIME_FORMAT.format(Instant.now().atZone(ZoneId.systemDefault()));
        Span timestamp = new Span(time);
        timestamp.addClassName("tg-time");

        Div meta = new Div(author, timestamp);
        meta.addClassName("tg-meta");

        Span text = new Span("Оператор думает...");
        text.addClassName("tg-text");

        Div bubble = new Div(meta, text);
        bubble.addClassNames("tg-bubble", "tg-bot", "tg-typing");

        row.add(bubble);
        messages.add(row);
        typingIndicator = row;
    }

    private void hideTypingIndicator() {
        if (typingIndicator != null) {
            typingIndicator.removeFromParent();
            typingIndicator = null;
        }
    }

    private void setComposerEnabled(boolean enabled) {
        input.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    private void scrollToBottom() {
        UI.getCurrent().getPage().executeJs(
                "const el = $0; el.scrollTop = el.scrollHeight;", messages.getElement()
        );
    }
}
