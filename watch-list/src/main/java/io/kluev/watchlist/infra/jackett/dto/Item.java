package io.kluev.watchlist.infra.jackett.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Item {
    @XmlElement
    @JacksonXmlProperty
    private String title;
    private String guid;
    private String type;
    private String comments;
    private String pubDate;
    private Long size;
    private Integer grabs;
    private String description;
    private String link;
    private List<String> category;
    private Enclosure enclosure;
    private List<Attr> attr;

    public String getAttr(String attrName) {
        return attr.stream()
                .filter(attr -> attr.getName().equals(attrName))
                .findFirst()
                .map(Attr::getValue)
                .orElse(null);
    }
}
