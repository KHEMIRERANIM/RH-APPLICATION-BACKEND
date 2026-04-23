import csv
import random

random.seed(42)

glutens = ['farine', 'pain', 'pates', 'couscous', 'semoule', 'orge', 'biscuit', 'brioche']
lactoses = ['lait', 'fromage', 'beurre', 'creme', 'yaourt', 'mascarpone', 'mozzarella', 'parmesan']
oeufs_list = ['oeuf', 'mayonnaise']
poissons = ['saumon', 'thon', 'sardine', 'cabillaud', 'bar']
fruits_mer = ['crevette', 'moule', 'homard', 'calamar', 'crabe']
viandes = ['boeuf', 'poulet', 'agneau', 'veau', 'dinde']
legumes = ['tomate', 'salade', 'concombre', 'carotte', 'courgette', 'aubergine', 'poivron', 'oignon', 'ail', 'epinard', 'champignon', 'pomme de terre']
epices = ['sel', 'poivre', 'cumin', 'paprika', 'curcuma', 'persil', 'thym']
autres = ['huile', 'sucre', 'miel', 'cafe', 'chocolat', 'citron', 'olive', 'noix']

rows = [['ingredients','gluten','lactose','oeufs','poisson','fruits_de_mer','vegetarien']]

for _ in range(2000):
    ingredients = []
    has_gluten = 0
    has_lactose = 0
    has_oeufs = 0
    has_poisson = 0
    has_fruits_mer = 0
    has_viande = 0

    ingredients += random.sample(legumes, random.randint(1, 3))
    ingredients += random.sample(epices, random.randint(1, 2))

    r = random.random()
    if r < 0.15:
        ingredients += random.sample(glutens, random.randint(1, 2))
        has_gluten = 1
    elif r < 0.30:
        ingredients += random.sample(lactoses, random.randint(1, 2))
        has_lactose = 1
    elif r < 0.40:
        ingredients.append(random.choice(oeufs_list))
        has_oeufs = 1
    elif r < 0.50:
        ingredients.append(random.choice(poissons))
        has_poisson = 1
    elif r < 0.60:
        ingredients.append(random.choice(fruits_mer))
        has_fruits_mer = 1
    elif r < 0.75:
        ingredients.append(random.choice(viandes))
        has_viande = 1

    if random.random() < 0.3:
        ingredients += random.sample(glutens, 1)
        has_gluten = 1
    if random.random() < 0.2:
        ingredients += random.sample(lactoses, 1)
        has_lactose = 1
    if random.random() < 0.15:
        ingredients.append(random.choice(oeufs_list))
        has_oeufs = 1

    ingredients += random.sample(autres, random.randint(0, 2))
    ingredients = list(set(ingredients))
    random.shuffle(ingredients)

    vegetarien = 0 if (has_viande or has_poisson or has_fruits_mer) else 1

    rows.append([
        ' '.join(ingredients),
        str(has_gluten), str(has_lactose), str(has_oeufs),
        str(has_poisson), str(has_fruits_mer), str(vegetarien)
    ])

with open('dataset_allergenes.csv', 'w', newline='', encoding='utf-8') as f:
    csv.writer(f).writerows(rows)

print(f'Dataset cree: {len(rows)-1} exemples')