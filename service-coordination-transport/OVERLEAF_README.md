# Compiler la présentation G4 sur Overleaf

## Fichiers obligatoires (même dossier que `presentation.tex`)

Uploader **tous** ces fichiers :

| Fichier |
|---------|
| `presentation.tex` (ou `PRESENTATION_G4_SGITU.tex` renommé) |
| `beamerthemeuae.sty` |
| `beamercolorthemeuae.sty` |
| `beamerinnerthemeuae.sty` |
| `beamerouterthemeuae.sty` |

## Optionnel

- `logo_uae_ensa.png` ou `image.png` (logo ENSA)
- Captures dans `figures/` (sinon les cadres gris suffisent)

## Erreur corrigée

`beamerthemeuae.sty not found` → les 4 fichiers `.sty` ci-dessus sont dans `service-coordination-transport/`.

## Si `output.pdf` existe sur Overleaf

Renommer ou supprimer `output.pdf` puis recompiler.

## Compilateur

**pdfLaTeX** (menu Overleaf : Compiler = pdfLaTeX).
