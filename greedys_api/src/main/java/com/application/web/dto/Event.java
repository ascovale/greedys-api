package com.application.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class Event{
	
	private String id;
	private String restaurant;
	private String title;	
	private String start;
	private String end;
	private String url;
	private String color="red";
	private String pax;
	
	private void generateUrl(Long idReservation) {
		this.url="http://localhost:5050/calendar_info"+"?idReservation="+idReservation;
	}

	public Event(Long id,
			LocalDate start,
            LocalDate end, 
            Integer pax, 
            String company, 
            String menu,
            Long idReservation,
            LocalTime time,
            String restaurant, 
            Boolean restaurantApproved, 
            Boolean restaurantDeleted
            ) {	
		this.id = id.toString();
		this.start=start.toString()+"T"+time.toString();
		this.restaurant=restaurant;
		//System.out.println("START = "+this.start.toString());
		setTitle(company, pax, menu);
		generateUrl(idReservation);
		this.pax=pax.toString();
		if (restaurantDeleted!= null) {
			color="red";
		}else if (restaurantApproved) {
			color="green";
		} else {
			color="yellow";
		}
	}

	public void setTitle(String company, Integer pax, String menu) {
		final StringBuilder builder = new StringBuilder();
		builder.append(pax).append(" pax, ")
				.append(company).append(", "+menu);
		this.title= builder.toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {	
		this.end = end;
	}
	/*
	private String getStringDate(int year, int month, int day, int hour, int minute) {
		System.out.println("    --->>>>"+getStringValue(year)+"-"+getStringValue(month)+
				"-"+getStringValue(day)+"T"+getStringValue(hour)+":"+getStringValue(minute)+":"+"00");
		return getStringValue(year)+"-"+getStringValue(month)+
				"-"+getStringValue(day);
	}
	
	private String getStringValue(Integer value){
	String strValue;
	strValue=value.toString();
	if(value<10)
		strValue = "0"+value;
	return strValue;
	}*/
	
	public String getTotalString(){
		//String str = "[{\"title+\":\"evento1\"," + "\"start\":\"2021-04-10\"}]";
		String str = "\"title\":\""
				+ title + "\","
				+ "\"start\":\""
				+ start+"\"";
		//System.out.println("Stringa ="+ str);
		return str;
		
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}


	public String getRestaurant() {
		return restaurant;
		
	}

	public void setIdRestaurant(String restaurant) {
		this.restaurant = restaurant;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String eventColor) {
		this.color = eventColor;
	}

	public String getPax() {
		return pax;
	}

	public void setPax(String pax) {
		this.pax = pax;
	}
}
