package com.jdev.jsprite;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpriteRequestTest {
    @Test
    public void should_add_files_to_list() {
        final SpriteRequest spriteRequest = new SpriteRequest();
        spriteRequest.setFileList(new File[]{
                new File("0"),
                new File("1")
        });
        spriteRequest.appendToFileList(new File[]{
                new File("2"),
                new File("3"),
                new File("4"),
                new File("5")
        });

        assertThat(spriteRequest.getFileList().length, is(6));
        assertThat(spriteRequest.getFileList()[0].getName(), is("0"));
        assertThat(spriteRequest.getFileList()[1].getName(), is("1"));
        assertThat(spriteRequest.getFileList()[2].getName(), is("2"));
        assertThat(spriteRequest.getFileList()[3].getName(), is("3"));
        assertThat(spriteRequest.getFileList()[4].getName(), is("4"));
        assertThat(spriteRequest.getFileList()[5].getName(), is("5"));
    }
}
