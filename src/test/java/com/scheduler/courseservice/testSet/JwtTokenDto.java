package com.scheduler.courseservice.testSet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
@Builder
public class JwtTokenDto {
	private String accessToken;
	private String refreshToken;
	private Date expiresDate;
}

