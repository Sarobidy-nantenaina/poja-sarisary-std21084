package school.hei.sary.endpoint.rest.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import school.hei.sary.PojaGenerated;
import school.hei.sary.file.BucketComponent;
@PojaGenerated
@RestController
@AllArgsConstructor
public class SarisaryController {
  BucketComponent bucketComponent;
  private static final String BLACK_WHITE_KEY = "blackWhiteImages/";
  private static final String PNG_EXTENSION = ".png";

  @PutMapping("/black-and-white/{id}")
  public ResponseEntity<String> convertToBlackAndWhite(@PathVariable String id, @RequestBody MultipartFile file) {
    try {
      String fileId = UUID.randomUUID().toString();

      String colorImageBucketKey = BLACK_WHITE_KEY + id + "/" + fileId + "_color" + PNG_EXTENSION;

      File uploadedColorImage = convertAndUploadImage(file, colorImageBucketKey);

      String blackAndWhiteImageBucketKey = BLACK_WHITE_KEY + id + "/" + fileId + "_bw" + PNG_EXTENSION;

      File uploadedBlackAndWhiteImage = convertAndUploadImage(file, blackAndWhiteImageBucketKey);

      String blackAndWhiteImageUrl = getPresignedUrl(blackAndWhiteImageBucketKey);
      return ResponseEntity.ok(blackAndWhiteImageUrl);
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during image conversion.");
    }
  }

  @GetMapping("/black-and-white/{id}")
  public ResponseEntity<Map<String, String>> getBlackAndWhiteImageUrls(@PathVariable String id) {
    try {
      String colorImageBucketKey = BLACK_WHITE_KEY + id + "_color" + PNG_EXTENSION;

      String colorImageUrl = getPresignedUrl(colorImageBucketKey);

      String blackAndWhiteImageBucketKey = BLACK_WHITE_KEY + id + "_bw" + PNG_EXTENSION;

      String blackAndWhiteImageUrl = getPresignedUrl(blackAndWhiteImageBucketKey);

      Map<String, String> imageUrls = new HashMap<>();
      imageUrls.put("original_url", colorImageUrl);
      imageUrls.put("transformed_url", blackAndWhiteImageUrl);

      return ResponseEntity.ok(imageUrls);
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  private File convertAndUploadImage(MultipartFile file, String bucketKey) throws IOException {
    // Lire l'image en mémoire
    BufferedImage image = ImageIO.read(file.getInputStream());

    // Convertir l'image en noir et blanc (binarisation avec un seuil)
    BufferedImage bwImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D g = bwImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();

    File tempFile = File.createTempFile("bw_", PNG_EXTENSION);
    ImageIO.write(bwImage, "png", tempFile);

    bucketComponent.upload(tempFile, bucketKey);

    return tempFile;
  }

  private String getPresignedUrl(String bucketKey) throws IOException {
    URL presignedUrl = bucketComponent.presign(bucketKey, Duration.ofMinutes(2));
    return presignedUrl.toString();
  }

}
