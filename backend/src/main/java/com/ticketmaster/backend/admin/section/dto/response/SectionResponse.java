package com.ticketmaster.backend.admin.section.dto.response;

import com.ticketmaster.backend.domain.seat.entity.Section;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SectionResponse {

    private Long sectionId;
    private String name;
    private Integer displayOrder;
    private String description;

    public static SectionResponse from(Section s) {
        return new SectionResponse(
                s.getId(),
                s.getName(),
                s.getDisplayOrder(),
                s.getDescription()
        );
    }
}
