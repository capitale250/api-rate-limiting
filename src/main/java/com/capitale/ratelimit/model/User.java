package com.capitale.ratelimit.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name="user_rate")
@NoArgsConstructor
public class User implements Serializable{

	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;
	
	private String name;
	
	private int rate_limit;

	public User(int id,String name, int i) {
		this.id=id;
		this.name=name;
		this.rate_limit=i;
	}
}
