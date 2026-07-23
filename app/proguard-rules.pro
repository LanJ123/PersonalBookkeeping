# kotlinx.serialization generates serializers that are referenced from companion
# objects. The library ships consumer rules; retaining annotations keeps the
# serialized backup contract inspectable after optimization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Room, Compose, Navigation, DataStore and Biometric ship consumer rules. Do not
# add broad package keeps here: an optimized candidate must prove those rules are
# sufficient through Release installation and end-to-end tests.
