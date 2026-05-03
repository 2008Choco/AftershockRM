package wtf.choco.aftershock.schema;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import wtf.choco.aftershock.files.ReplayMetadataAccessor;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.structure.ReplayBin;

import java.util.List;

public final class AftershockTypeAdapterFactory implements TypeAdapterFactory {

    public static final TypeAdapterFactory INSTANCE = new AftershockTypeAdapterFactory();

    // Register type adapters in this map :)
    private static final List<TypeAdapterRegistration<?>> TYPE_ADAPTERS = List.of(
            TypeAdapterRegistration.create(ReplayMetadata.class, ReplayMetadataTypeAdapter::new),
            TypeAdapterRegistration.create(ReplayMetadataAccessor.class, ReplayMetadataStoreTypeAdapter::new),
            TypeAdapterRegistration.create(ReplayBin.class, ReplayBinTypeAdapter::new)
    );

    private AftershockTypeAdapterFactory() { }

    // <editor-fold desc="Registration boilerplate">
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        for (TypeAdapterRegistration<?> registration : TYPE_ADAPTERS) {
            if (registration.clazz().isAssignableFrom(type.getRawType())) {
                return (TypeAdapter<T>) registration.instantiator().create(gson);
            }
        }

        return null;
    }

    @FunctionalInterface
    private interface TypeAdapterInstantiator<T> {

        public TypeAdapter<T> create(Gson gson);

        @FunctionalInterface
        interface Simple<T> extends TypeAdapterInstantiator<T> {

            @Override
            public default TypeAdapter<T> create(Gson gson) {
                return create();
            }

            public TypeAdapter<T> create();

        }

    }

    private static final class MemoizedTypeAdapterInstantiator<T> implements TypeAdapterInstantiator<T> {

        private TypeAdapter<T> typeAdapter;

        private final TypeAdapterInstantiator<T> delegate;

        private MemoizedTypeAdapterInstantiator(TypeAdapterInstantiator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public TypeAdapter<T> create(Gson gson) {
            if (typeAdapter == null) {
                this.typeAdapter = delegate.create(gson);
            }

            return typeAdapter;
        }

    }

    private record TypeAdapterRegistration<T>(Class<T> clazz, TypeAdapterInstantiator<T> instantiator) {

        public static <T> TypeAdapterRegistration<T> create(Class<T> clazz, TypeAdapterInstantiator<T> instantiator) {
            return new TypeAdapterRegistration<>(clazz, new MemoizedTypeAdapterInstantiator<>(instantiator));
        }

        public static <T> TypeAdapterRegistration<T> create(Class<T> clazz, TypeAdapterInstantiator.Simple<T> instantiator) {
            return create(clazz, (TypeAdapterInstantiator<T>) instantiator);
        }

    }
    // </editor-fold>

}
