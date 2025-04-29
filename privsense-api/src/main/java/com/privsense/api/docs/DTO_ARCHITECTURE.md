# Architecture des DTOs dans PrivSense

Ce document décrit l'architecture des Data Transfer Objects (DTOs) utilisés dans l'API PrivSense. Il sert de référence pour comprendre la structure, les hiérarchies et les bonnes pratiques.

## Structure générale

L'architecture des DTOs est conçue autour d'une hiérarchie de classes de base qui standardisent la structure et le comportement des objets de transfert de données. 

```
BaseRequestDTO
 ├── DatabaseConnectionRequest
 ├── ScanRequest
 ├── BatchSamplingRequest
 └── SamplingRequest

BaseResponseDTO
 ├── ConnectionResponse
 ├── SchemaInfoDTO
 ├── ScanJobResponse
 ├── ComplianceReportDTO
 ├── SamplingResponse
 └── BatchSamplingResponse
```

## Classes de base

### BaseRequestDTO
La classe de base pour toutes les requêtes entrantes. Elle peut contenir des métadonnées communes, des champs d'audit, etc.

### BaseResponseDTO
La classe de base pour toutes les réponses. Elle contient :
- `status` : État de la réponse (SUCCESS, ERROR)
- `errorMessage` : Message d'erreur (si applicable)

## Sous-packages et organisation

Les DTOs sont organisés dans les sous-packages suivants :

- `com.privsense.api.dto.base` : Classes de base
- `com.privsense.api.dto.config` : DTOs de configuration
- `com.privsense.api.dto.result` : DTOs pour les résultats d'opérations

## Validation

Les validations sont standardisées à l'aide d'annotations Jakarta Validation :
- `@NotNull`, `@NotBlank`, `@NotEmpty` pour les champs obligatoires
- `@Size`, `@Min`, `@Max` pour les contraintes de taille
- `@Threshold` (custom) pour valider les valeurs entre 0 et 1

## Mappers

Les conversions entre entités et DTOs sont gérées par MapStruct dans les classes suivantes :

- `DtoMapper` : Conversions générales entre DTOs et modèles
- `EntityMapper` : Conversions spécifiques aux entités
- `SchemaMapper` : Conversions pour les métadonnées de schéma
- `DetectionMapper` : Conversions pour les résultats de détection PII

## Avantages de cette architecture

1. **Cohérence** : Structure uniforme pour toutes les réponses API
2. **Gestion des erreurs simplifiée** : Mécanisme standardisé via BaseResponseDTO
3. **Évolutivité** : Facilité d'ajout de nouveaux champs communs
4. **Typage fort** : Hiérarchie claire avec héritage
5. **Optimisation des performances** : MapStruct génère du code optimisé à la compilation

## Bonnes pratiques

1. Tous les nouveaux DTOs de requête doivent étendre `BaseRequestDTO`
2. Tous les nouveaux DTOs de réponse doivent étendre `BaseResponseDTO`
3. Utilisez des annotations de validation pour documenter les contraintes
4. Préférez l'immutabilité quand c'est possible (Builder pattern)
5. Évitez d'exposer directement les entités JPA dans l'API
6. Utilisez les mappers pour la conversion entre entités et DTOs
7. Documentez les champs et les classes avec des annotations Swagger

## Classes extraites pour la modularité

Certaines classes internes ont été extraites dans leurs propres fichiers pour améliorer la modularité :

- `ColumnSamplingResult` : Résultats d'échantillonnage pour une colonne
- `TableSamplingResult` : Résultats d'échantillonnage pour une table
- `DetectionResultDTO` : Résultats de détection PII

## Validation personnalisée

L'annotation `@Threshold` simplifie la validation des valeurs entre 0 et 1, couramment utilisées pour les seuils de confiance.