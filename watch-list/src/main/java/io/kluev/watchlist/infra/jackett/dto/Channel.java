package io.kluev.watchlist.infra.jackett.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Channel {
    @Setter
    private AtomLink atomLink;
    private String title;
    private String description;
    private String link;
    private String language;
    private String category;
    private List<Item> item;

    @XmlElement
    @JacksonXmlProperty(localName = "atom:link")
    public AtomLink getAtomLink() {
        return atomLink;
    }

    @XmlElement
    @JacksonXmlProperty(localName = "item")
    public List<Item> getItem() {
        if (item == null) {
            item = new ArrayList<>();
        }
        return item;
    }

}
