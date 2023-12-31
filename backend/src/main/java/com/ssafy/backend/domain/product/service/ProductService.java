package com.ssafy.backend.domain.product.service;

import com.ssafy.backend.domain.product.ProductImage;
import com.ssafy.backend.domain.product.ProductType;
import com.ssafy.backend.domain.product.SeenProduct;
import com.ssafy.backend.domain.product.dto.ProductDetailDto;
import com.ssafy.backend.domain.product.dto.ProductDetailResult;
import com.ssafy.backend.domain.product.dto.ProductResultDto;
import com.ssafy.backend.domain.product.repository.ProductRepository;
import com.ssafy.backend.domain.product.repository.SeenProductRepository;
import com.ssafy.backend.domain.user.User;
import com.ssafy.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final RedisTemplate redisTemplate;
	private final SeenProductRepository seenProductRepository;

	public Slice<ProductResultDto> getTypeProduct(ProductType productType, int page, int size) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalArgumentException("JWT token: 회원 이메일에 해당하는 회원이 없습니다."));

		PageRequest pageRequest = PageRequest.of(page, size);

		return productRepository.getProductWithWish(user, productType, pageRequest);
	}

	public ProductDetailDto getProductDetail(long productId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalArgumentException("JWT token: 회원 이메일에 해당하는 회원이 없습니다."));

		ProductDetailResult productDetailResult = productRepository.getProductDetail(user, productId);
		List<String> images = productDetailResult.getProduct().getProductImages().stream()
				.map(ProductImage::getImage)
				.collect(Collectors.toList());

		// mysql 시간 비교 위해 추가
		seenProductRepository.save(new SeenProduct(user.getId(), productDetailResult.getProduct()));

		//redis: 상품 상세조회 후 최근 상품 보기 리스트에 추가
		try{
			System.out.println("=============redis");
			String key = "latest-seen-products:" + Long.valueOf(user.getId());
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);
			sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); //이걸 뺴먹음
			redisTemplate.opsForZSet().add(key, String.valueOf(productId), Long.valueOf(sdf.format(timestamp))); //되ㅏ나
			System.out.println("저장 완료");
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("저장 실패");
		}
		finally {
			System.out.println("=============redis");
		}

		return new ProductDetailDto(productDetailResult.getProduct(), productDetailResult.isWish(), images);
	}

}
