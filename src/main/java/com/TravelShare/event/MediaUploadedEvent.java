package com.TravelShare.event;

import com.TravelShare.entity.Media;
import com.TravelShare.entity.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MediaUploadedEvent extends ApplicationEvent {
    Media media;
    User uploader;

    public MediaUploadedEvent(Object source, Media media, User uploader) {
        super(source);
        this.media = media;
        this.uploader = uploader;
    }
}
