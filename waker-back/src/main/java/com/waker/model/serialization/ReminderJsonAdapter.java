package com.waker.model.serialization;

import com.google.gson.*;
import com.waker.model.Reminder;
import com.waker.model.dto.ReminderDTO;
import com.waker.model.penalty.APenalty;

import java.lang.reflect.Type;

public class ReminderJsonAdapter implements JsonDeserializer<ReminderDTO> {

    @Override
    public ReminderDTO deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement penaltySettingElement = jsonObject.get("penaltySetting");
        Gson gson = new Gson();

        try {
            JsonObject reminderObjectNoPenalty = jsonObject.deepCopy();
            String penaltyType = penaltySettingElement.getAsJsonObject().get("_class").getAsString();
            reminderObjectNoPenalty.remove("penaltySetting");
            APenalty penalty = context.deserialize(penaltySettingElement, Class.forName("com.waker.model.penalty.impl." + penaltyType));
            ReminderDTO reminder = gson.fromJson(reminderObjectNoPenalty, ReminderDTO.class);

            reminder.setPenaltySetting(penalty);
            return reminder;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
