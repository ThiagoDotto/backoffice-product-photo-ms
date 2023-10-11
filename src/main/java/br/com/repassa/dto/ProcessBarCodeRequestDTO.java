package br.com.repassa.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessBarCodeRequestDTO {
	private String data;
	private List<GroupPhoto> groupPhotos;

	@Getter
	@Setter
	public static class GroupPhoto {
		private String id;
		private Photo photos;
	}

	@Getter
	@Setter
	public static class Photo {
		private String idPhoto;
		private String urlPhotoBarCode;
	}
}
