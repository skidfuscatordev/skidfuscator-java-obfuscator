package framework.testclasses;

import framework.Pure;

import java.time.LocalDateTime;

public class ImmutableConfiguration {
    private final String name;
    private final int maxSize;
    private final LocalDateTime validUntil;
    private final ImmutableTree<String> dataStructure;

    public static class Builder {
        private String name = "";
        private int maxSize = 100;
        private LocalDateTime validUntil = LocalDateTime.now().plusDays(1);
        private ImmutableTree<String> dataStructure = ImmutableTree.leaf("");

        @Pure(description = "Pure builder method",
             because = {"Modifies only builder state", "Fluent interface"})
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        @Pure(description = "Pure builder method",
             because = {"Modifies only builder state", "Fluent interface"})
        public Builder withMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        @Pure(description = "Pure builder method",
             because = {"Modifies only builder state", "Fluent interface"})
        public Builder withValidUntil(LocalDateTime validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        @Pure(description = "Pure builder method",
             because = {"Modifies only builder state", "Fluent interface"})
        public Builder withDataStructure(ImmutableTree<String> dataStructure) {
            this.dataStructure = dataStructure;
            return this;
        }

        @Pure(description = "Creates immutable object",
             because = {"Returns new instance", "Builder state isolated"})
        public ImmutableConfiguration build() {
            return new ImmutableConfiguration(this);
        }
    }

    private ImmutableConfiguration(Builder builder) {
        this.name = builder.name;
        this.maxSize = builder.maxSize;
        this.validUntil = builder.validUntil;
        this.dataStructure = builder.dataStructure;
    }

    @Pure(description = "Pure validation method",
         because = {"Only reads state", "No side effects"})
    public boolean isValid() {
        return !name.isEmpty() && 
               maxSize > 0 && 
               validUntil.isAfter(LocalDateTime.now()) &&
               dataStructure.countNodes() <= maxSize;
    }
}