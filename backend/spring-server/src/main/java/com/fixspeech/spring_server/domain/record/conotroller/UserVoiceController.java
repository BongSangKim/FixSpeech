package com.fixspeech.spring_server.domain.record.conotroller;

import java.io.InputStream;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fixspeech.spring_server.domain.grass.service.GrassService;
import com.fixspeech.spring_server.domain.record.dto.UserVoiceListResponseDto;
import com.fixspeech.spring_server.domain.record.service.UserVoiceService;
import com.fixspeech.spring_server.domain.user.model.Users;
import com.fixspeech.spring_server.domain.user.service.UserService;
import com.fixspeech.spring_server.global.common.ApiResponse;
import com.fixspeech.spring_server.global.exception.CustomException;
import com.fixspeech.spring_server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/record")
public class UserVoiceController implements UserVoiceApi {
	private final UserVoiceService userVoiceService;
	private final UserService userService;
	private final GrassService grassService;

	@PostMapping
	public ApiResponse<?> analyze(
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestPart(value = "record", required = false) MultipartFile file
	) {

		Users users = userService.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Map<String, Object> result = userVoiceService.analyzeAndSave(users, file);
		grassService.addGrassRecord(users.getId());
		return ApiResponse.createSuccess(result, "사용자 녹음 파일 분석 성공");

	}

	//리스트 목록
	@GetMapping
	public ApiResponse<?> getUserRecordList(
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Users user = userService.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Page<UserVoiceListResponseDto> result = userVoiceService.getUserRecordList(page, size, user.getId());
		return ApiResponse.createSuccess(result, "사용자 분석 결과 목록 조회 성공");

	}

	//최신 음성 분석 단일 조회
	@GetMapping("/recent")
	public ApiResponse<?> getRecentRecord(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		Users users = userService.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		UserVoiceListResponseDto result = userVoiceService.getRecent(users);
		return ApiResponse.createSuccess(result, "최신 유저 음성 데이터 조회 성공");
	}

	//음성 분석 단일 조회
	@GetMapping("/{recordId}")
	public ApiResponse<?> getUserRecordDetail(
		@AuthenticationPrincipal UserDetails userDetails,
		@PathVariable Long recordId
	) {
		Users users = userService.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		UserVoiceListResponseDto result = userVoiceService.getUserRecordDetail(users, recordId);
		return ApiResponse.createSuccess(result, "음성분석 상세 조회");
	}

	//음성 분석 단일 삭제
	@DeleteMapping("/{recordId}")
	public ApiResponse<?> deleteUserRecord(
		@AuthenticationPrincipal UserDetails userDetails,
		@PathVariable Long recordId
	) {
		Users users = userService.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		userVoiceService.deleteRecord(users, recordId);
		return ApiResponse.success("유저 음성 삭제 완료");
	}

	// MultipartFile의 InputStream을 처리하기 위한 헬퍼 클래스
	public static class MultipartInputStreamFileResource extends InputStreamResource {
		private final String filename;

		public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
			super(inputStream);
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return this.filename;
		}

		@Override
		public long contentLength() {
			return -1; // 길이를 알 수 없음을 나타냄
		}
	}

}
