package wtf.choco.aftershock.util;

import javafx.scene.Node;

public record LoadedFXMLObject<T extends Node, C>(T root, C controller) { }
