package com.optoma.meeting.model.azureopenai.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChatResponse {

   @SerializedName("choices")
   @Expose
   public List<ChatChoices> choices;
}
